package io.gitlab.icestom.icestom.instance;

import io.github.openboatutils.protocol.OBUPacket;
import io.github.openboatutils.protocol.channels.OBUContextPacket;
import io.github.openboatutils.protocol.channels.OBUSettingsPacket;
import io.gitlab.icestom.icestom.IceStom;
import io.gitlab.icestom.icestom.entity.IceStomPlayer;
import io.gitlab.icestom.icestom.track.Track;
import io.gitlab.icestom.icestom.track.TickMovement;
import io.gitlab.icestom.icestom.track.colliders.CrossCollider;
import io.gitlab.icestom.icestom.track.colliders.InsideCollider;
import io.gitlab.icestom.stomtrack.EnvironmentFile;
import net.hollowcube.polar.PolarLoader;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.event.item.ItemDropEvent;
import net.minestom.server.event.player.PlayerBlockBreakEvent;
import net.minestom.server.event.player.PlayerBlockPlaceEvent;
import net.minestom.server.instance.LightingChunk;
import net.minestom.server.registry.DynamicRegistry;
import net.minestom.server.registry.RegistryKey;
import net.minestom.server.world.DimensionType;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

import static io.gitlab.icestom.icestom.openboatutils.OpenBoatUtilsManager.writePacket;

@SuppressWarnings("UnstableApiUsage")
public abstract class TrackInstance extends BoatInstance implements SpawnLocation {

    private static final Logger log = LoggerFactory.getLogger(TrackInstance.class);
    protected final Track track;
    private final Map<Player, Vec> lastTickPositions = new HashMap<>();

    private final Set<String> subscribedRegions = new HashSet<>();
    private final Set<String> subscribedTriggers = new HashSet<>();

    private Set<InsideCollider> watchingRegions = Set.of();
    private Set<CrossCollider> watchingTriggers = Set.of();

    public TrackInstance(Track track) {
        super(Key.key(IceStom.NAMESPACE, "track/" + track.getEnvironmentId()), getDimensionKey(track.getEnvironmentData()));

        this.track = track;

        setChunkSupplier(LightingChunk::new);
        setChunkLoader(new PolarLoader(track.getWorld()));

        eventNode().addListener(PlayerBlockBreakEvent.class, event -> event.setCancelled(true));
        eventNode().addListener(PlayerBlockPlaceEvent.class, event -> event.setCancelled(true));
    }

    @Override
    public void tick(long time) {
        super.tick(time);

        Map<Player, TickMovement> movementMap = new HashMap<>();

        for (Player player : getPlayers()) {
            if (!shouldTrackPlayer(player)) {
                lastTickPositions.remove(player);
                continue;
            }

            Vec current = player.getPosition().asVec();
            @Nullable Vec last = lastTickPositions.get(player);

            movementMap.put(player, new TickMovement(last, current));

            lastTickPositions.put(player, current);
        }

        Map<Player, Set<String>> inside_tags = new HashMap<>();
        Map<Player, Map<String, Long>> crossed_triggers = new HashMap<>();

        // TODO: can probably micro-optimise this by avoiding redundant checks on region tags a player is already in

        var regions = track.getRegions();
        for (InsideCollider watchingRegion : watchingRegions) {
            for (Player player : watchingRegion.detectInside(movementMap)) {
                inside_tags.computeIfAbsent(player, _ -> new HashSet<>()).addAll(regions.get(watchingRegion));
            }
        }

        var triggers = track.getTriggers();
        for (CrossCollider watchingTrigger : watchingTriggers) {
            watchingTrigger.detectCrosses(movementMap).forEach((player, delta) -> {
                Map<String, Long> crosses = crossed_triggers.computeIfAbsent(player, _ -> new HashMap<>());

                triggers.get(watchingTrigger).forEach(string -> {
                    crosses.put(string, delta);
                });
            });
        }

        onPlayerMovements(movementMap, inside_tags, crossed_triggers);
    }

    @Override
    public void resetPlayer(Player player) {
        SpawnLocation.super.resetPlayer(player);

        if (!track.getOpenBoatUtilsPackets().isEmpty()) {
            if (((IceStomPlayer) player).getOpenBoatUtilsVersion() == null) {
                player.sendMessage(Component.translatable("message.timetrial.requires_open_boat_utils"));
                drop(player);
                IceStom.getInstance().getSpawnInstance().consume(player);
            } else {
                try {
                    List<OBUSettingsPacket> packets = new ArrayList<>();
                    packets.add(new OBUSettingsPacket.Reset());
                    packets.addAll(track.getOpenBoatUtilsPackets());

                    OBUPacket compound = new OBUSettingsPacket.Compound(new OBUSettingsPacket.CompoundPayload(packets));

                    player.sendPacket(writePacket(compound));
                } catch (IOException _) {}
            }
        }
    }

    @Override
    public void drop(Player player) {
        removeBoat(player);
        try {
            player.sendPacket(writePacket(new OBUContextPacket.Reset()));
        } catch (IOException _) {}
    }

    protected abstract void onPlayerMovements(Map<Player, TickMovement> movements, Map<Player, Set<String>> inside_tags, Map<Player, Map<String, Long>> crossed_triggers);
    protected abstract boolean shouldTrackPlayer(Player player);

    public Track getTrack() {
        return track;
    }

    private void updateWatchedRegions() {
        Map<InsideCollider, Set<String>> colliders = new HashMap<>(track.getRegions());

        colliders.entrySet().removeIf(entry -> {
            for (String tag: entry.getValue()) {
                if (subscribedRegions.contains(tag)) return false;
            }

            return true;
        });

        watchingRegions = colliders.keySet();
    }

    private void updateWatchedTriggers() {
        Map<CrossCollider, Set<String>> colliders = new HashMap<>(track.getTriggers());

        colliders.entrySet().removeIf(entry -> {
            for (String tag: entry.getValue()) {
                if (subscribedTriggers.contains(tag)) return false;
            }

            return true;
        });

        watchingTriggers = colliders.keySet();
    }

    public void subscribeRegionId(String id) {
        this.subscribedRegions.add(id);
        updateWatchedRegions();
    }

    public boolean unsubscribeRegionId(String id) {
        boolean removed = this.subscribedRegions.remove(id);
        updateWatchedRegions();
        return removed;
    }

    public void subscribeTriggerId(String id) {
        this.subscribedTriggers.add(id);
        updateWatchedTriggers();
    }

    public boolean unsubscribeTriggerId(String id) {
        boolean removed = this.subscribedTriggers.remove(id);
        updateWatchedRegions();
        return removed;
    }

    public static RegistryKey<DimensionType> getDimensionKey(EnvironmentFile environmentFile) {

        DynamicRegistry<DimensionType> registry = MinecraftServer.getDimensionTypeRegistry();

        String id = "d" + Objects.hash(
                environmentFile.getAmbientLight(),
                environmentFile.getHeight(),
                environmentFile.getMinY(),
                environmentFile.getHeight(),
                environmentFile.getSkybox()
        );

        Key key = Key.key(IceStom.NAMESPACE, id);

        @Nullable RegistryKey<DimensionType> pre_existing = registry.getKey(key);

        if (pre_existing != null) return pre_existing;

        DimensionType.Builder builder = DimensionType.builder();

        builder.minY(environmentFile.getMinY());
        builder.height(environmentFile.getHeight());
        builder.logicalHeight(environmentFile.getHeight());
        builder.skybox(switch (environmentFile.getSkybox()) {
            case OVERWORLD -> DimensionType.Skybox.OVERWORLD;
            case END -> DimensionType.Skybox.END;
            case NONE -> DimensionType.Skybox.NONE;
        });
        builder.ambientLight(environmentFile.getAmbientLight());
        builder.cardinalLight(environmentFile.getNetherLight() ? DimensionType.CardinalLight.NETHER : DimensionType.CardinalLight.DEFAULT);

        try {
            return MinecraftServer.getDimensionTypeRegistry()
                    .register(key, builder.build());
        } catch (UnsupportedOperationException e) {
            log.warn("Couldn't find a suitable dimension type candidate for environment. (maybe preload failed?)");
            return DimensionType.OVERWORLD;
        }
    }
}
