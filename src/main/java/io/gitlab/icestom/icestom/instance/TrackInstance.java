package io.gitlab.icestom.icestom.instance;

import io.github.openboatutils.protocol.OBUPacket;
import io.github.openboatutils.protocol.channels.OBUContextPacket;
import io.github.openboatutils.protocol.channels.OBUSettingsPacket;
import io.gitlab.icestom.icestom.IceStom;
import io.gitlab.icestom.icestom.config.IceStomConfig;
import io.gitlab.icestom.icestom.entity.IceStomPlayer;
import io.gitlab.icestom.icestom.timetrial.lap.TimedLap;
import io.gitlab.icestom.icestom.track.Track;
import io.gitlab.icestom.icestom.track.TickMovement;
import io.gitlab.icestom.icestom.track.colliders.CrossCollider;
import io.gitlab.icestom.icestom.track.colliders.InsideCollider;
import io.gitlab.icestom.icestom.track.library.TrackLibrary;
import io.gitlab.icestom.stomtrack.EnvironmentFile;
import net.hollowcube.polar.PolarLoader;
import net.hollowcube.polar.PolarWorldAccess;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import net.minestom.server.event.instance.InstanceTickEvent;
import net.minestom.server.event.player.PlayerBlockBreakEvent;
import net.minestom.server.event.player.PlayerBlockPlaceEvent;
import net.minestom.server.event.player.PlayerPacketEvent;
import net.minestom.server.instance.Chunk;
import net.minestom.server.instance.LightingChunk;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.packet.client.common.ClientPongPacket;
import net.minestom.server.network.packet.client.play.ClientVehicleMovePacket;
import net.minestom.server.network.packet.server.common.PingPacket;
import net.minestom.server.network.packet.server.common.PingResponsePacket;
import net.minestom.server.registry.DynamicRegistry;
import net.minestom.server.registry.RegistryKey;
import net.minestom.server.world.DimensionType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static io.gitlab.icestom.icestom.openboatutils.OpenBoatUtilsManager.writePacket;
import static io.gitlab.icestom.icestom.util.DisplayEntityConverter.*;

@SuppressWarnings("UnstableApiUsage")
public abstract class TrackInstance extends BoatInstance implements SpawnLocation, PolarWorldAccess {

    private static final Logger log = LoggerFactory.getLogger(TrackInstance.class);

    protected final Track track;
    private final TrackLibrary.Ticket ticket;

    private final Map<Player, Vec> lastTickPositions = new HashMap<>();

    private final Set<String> subscribedRegions = new HashSet<>();
    private final Set<String> subscribedTriggers = new HashSet<>();

    private Set<InsideCollider> watchingRegions = Set.of();
    private Set<CrossCollider> watchingTriggers = Set.of();

    protected TrackInstance(TrackLibrary.Ticket ticket, Track track) {
        super(UUID.randomUUID(), track.getMapContainer());

        this.ticket = ticket;
        this.track = track;

        setChunkSupplier(LightingChunk::new);

        eventNode().addListener(PlayerBlockBreakEvent.class, event -> event.setCancelled(true));
        eventNode().addListener(PlayerBlockPlaceEvent.class, event -> event.setCancelled(true));

        eventNode().addListener(PlayerPacketEvent.class, event -> {
            final Player player = event.getPlayer();

            if (event.getPacket() instanceof ClientPongPacket(int number)) {
                player.sendMessage(Component.text(number, NamedTextColor.GOLD));
            } else if (event.getPacket() instanceof ClientVehicleMovePacket) {
                player.sendMessage(Component.text("VehicleMove", NamedTextColor.BLUE));
            }
        });
    }

    @Override
    public void loadChunkData(@NotNull Chunk chunk, @Nullable NetworkBuffer userData) {
        if (userData == null) {
            return;
        }

        CompoundBinaryTag root = userData.read(NetworkBuffer.NBT_COMPOUND);
        ListBinaryTag list = root.getList("entities");

        for (BinaryTag binaryTag : list) {
            CompoundBinaryTag nbt = (CompoundBinaryTag) binaryTag;

            String id = nbt.getString("id");

            Entity entity = switch (id) {
                case "minecraft:block_display" -> loadBlockDisplay(nbt);
                case "minecraft:item_display" -> loadItemDisplay(nbt);
                case "minecraft:text_display" -> loadTextDisplay(nbt);
                default -> null;
            };

            if (entity == null) {
                continue;
            }

            ListBinaryTag pos = nbt.getList("Pos");

            double x = pos.getDouble(0);
            double y = pos.getDouble(1);
            double z = pos.getDouble(2);

            entity.setInstance(this, new Pos(x, y, z));
        }
    }

    @Override
    public String getDimensionName() {
        return getInstanceContainer().getDimensionName();
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

        for (Player player : getPlayers()) {
            if (shouldTrackPlayer(player)) {
                player.sendPacket(new PingPacket((int) getWorldAge()));
            }
        }
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

    public TrackLibrary.Ticket getTicket() {
        return ticket;
    }

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



    public static void tickResetRegions(
            TrackInstance instance,
            Player player,
            Set<String> playerRegions,
            Map<String, Long> playerTriggers,
            TimedLap lap
    ) {
        boolean hitResetRegion = playerRegions != null && playerRegions.contains("icestom.reset");
        boolean hitResetTrigger = playerTriggers != null && playerTriggers.containsKey("icestom.reset");

        if (hitResetRegion || hitResetTrigger) {
            Track track = instance.getTrack();
            Pos reset_point = track.getLocations().getOrDefault("icestom.reset_" + lap.getLastReachedCheckpoint(), track.getSpawnLocation());

            instance.createBoat(player, reset_point);
        }
    }
}
