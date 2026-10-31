package io.gitlab.icestom.icestom.instance;

import io.github.openboatutils.protocol.OBUPacket;
import io.github.openboatutils.protocol.channels.OBUContextPacket;
import io.github.openboatutils.protocol.channels.OBUSettingsPacket;
import io.gitlab.icestom.icestom.IceStom;
import io.gitlab.icestom.icestom.entity.Boat;
import io.gitlab.icestom.icestom.entity.IceStomPlayer;
import io.gitlab.icestom.icestom.track.PlayerMovement;
import io.gitlab.icestom.icestom.track.Track;
import io.gitlab.icestom.icestom.track.colliders.CrossCollider;
import io.gitlab.icestom.icestom.track.colliders.InsideCollider;
import io.gitlab.icestom.icestom.track.event.TrackRegionEnterEvent;
import io.gitlab.icestom.icestom.track.event.TrackTriggerPassEvent;
import io.gitlab.icestom.icestom.track.library.TrackLibrary;
import io.gitlab.icestom.icestom.ui.event.GenericErrorMessageEvent;
import io.gitlab.icestom.icestom.util.BatchQueue;
import net.kyori.adventure.nbt.ListBinaryTag;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.instance.InstanceRegisterEvent;
import net.minestom.server.event.player.PlayerBlockBreakEvent;
import net.minestom.server.event.player.PlayerBlockPlaceEvent;
import net.minestom.server.event.player.PlayerPacketEvent;
import net.minestom.server.instance.LightingChunk;
import net.minestom.server.network.packet.client.play.ClientVehicleMovePacket;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static io.gitlab.icestom.icestom.intergrations.OpenBoatUtilsManager.writePacket;
import static io.gitlab.icestom.icestom.util.DisplayEntityConverter.*;

@SuppressWarnings("UnstableApiUsage")
public abstract class TrackInstance extends BoatInstance implements SpawnLocation {

    private static final Logger log = LoggerFactory.getLogger(TrackInstance.class);

    protected final Track track;
    private final TrackLibrary.Ticket ticket;

    private final BatchQueue<TickLocation> movementBatchQueue = new BatchQueue<>();
    private final Map<UUID, Integer> ticks = new ConcurrentHashMap<>();

    private final Set<String> subscribedRegions = new HashSet<>();
    private final Set<String> subscribedTriggers = new HashSet<>();

    private Map<InsideCollider, Set<String>> watchingRegions = Map.of();
    private Map<CrossCollider, Set<String>> watchingTriggers = Map.of();

    protected TrackInstance(TrackLibrary.Ticket ticket, Track track) {
        super(UUID.randomUUID(), track.getMap());

        this.ticket = ticket;
        this.track = track;

        setChunkSupplier(LightingChunk::new);

        eventNode().addListener(PlayerBlockBreakEvent.class, event -> event.setCancelled(true));
        eventNode().addListener(PlayerBlockPlaceEvent.class, event -> event.setCancelled(true));

        eventNode().addListener(InstanceRegisterEvent.class, event -> {
            track.getMap().onNewDisplayEntity(nbt -> {
                String id = nbt.getString("id");

                Entity entity = switch (id) {
                    case "minecraft:block_display" -> loadBlockDisplay(nbt);
                    case "minecraft:item_display" -> loadItemDisplay(nbt);
                    case "minecraft:text_display" -> loadTextDisplay(nbt);
                    default -> null;
                };

                if (entity == null) return;

                ListBinaryTag pos = nbt.getList("Pos");

                double x = pos.getDouble(0);
                double y = pos.getDouble(1);
                double z = pos.getDouble(2);

                entity.setInstance(this, new Pos(x, y, z));
            });
        });

        eventNode().addListener(PlayerPacketEvent.class, event -> {
            final Player player = event.getPlayer();

            if (!shouldTrackPlayer(player)) return;
            if(!(player.getVehicle() instanceof Boat)) return;
            if (!(event.getPacket() instanceof ClientVehicleMovePacket(Pos position, boolean onGround))) return;

            final UUID uuid = player.getUuid();

            int tick = ticks.merge(uuid, 1, Integer::sum);

            movementBatchQueue.add(new TickLocation(
                    player.getUuid(),
                    tick,
                    position.asVec(),
                    position.yaw()
            ));
        });
    }

    @Override
    public @NonNull String getDimensionName() {
        return getInstanceContainer().getDimensionName();
    }

    public long getPlayerTick(UUID player) {
        return ticks.getOrDefault(player, 0);
    }

    @Override
    public void tick(long time) {
        super.tick(time);

        if (!movementBatchQueue.isEmpty()) {
            List<TickLocation> movements = movementBatchQueue.drain();

            handleMovements(movements);
        }
    }

    @Override
    public void resetPlayer(Player player) {
        SpawnLocation.super.resetPlayer(player);

        if (!track.getOpenBoatUtilsPackets().isEmpty()) {
            if (((IceStomPlayer) player).getOpenBoatUtilsVersion() == null) {
                MinecraftServer.getGlobalEventHandler().call(new GenericErrorMessageEvent(player, Component.translatable("message.timetrial.requires_open_boat_utils")));
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

    protected abstract void handleMovements(List<TickLocation> movements);
    protected abstract boolean shouldTrackPlayer(Player player);

    public TrackLibrary.Ticket getTicket() {
        return ticket;
    }

    public Track getTrack() {
        return track;
    }

    protected final void dispatchTrackEvents(List<PlayerMovement> moves) {
        if (watchingRegions.isEmpty() && watchingTriggers.isEmpty()) return;
        if (moves.isEmpty()) return;

        GlobalEventHandler handler = MinecraftServer.getGlobalEventHandler();

        moves.forEach((movement) -> {
            Player player = MinecraftServer.getConnectionManager().getOnlinePlayerByUuid(movement.current().player());

            if (player == null) return;

            watchingRegions.forEach((region, tags) -> {
                if (!region.detectInside(movement)) return;

                for (String tag : tags) {
                    handler.call(new TrackRegionEnterEvent(tag, this, movement, player));
                }
            });

            watchingTriggers.forEach((collider, tags) -> {
                if (collider.detectCross(movement.before(), movement.current()) == null) return;

                for (String tag : tags) {
                    handler.call(new TrackTriggerPassEvent(tag, this, movement, player));
                }
            });
        });
    }

    private static <C> Map<C, Set<String>> filterSubscribed(Map<C, Set<String>> all, Set<String> subscribed) {
        Map<C, Set<String>> result = new HashMap<>();
        all.forEach((collider, tags) -> {
            Set<String> matching = new HashSet<>(tags);
            matching.retainAll(subscribed);
            if (!matching.isEmpty()) result.put(collider, matching);
        });
        return result;
    }

    private void updateWatchedRegions() {
        watchingRegions = filterSubscribed(track.getRegions(), subscribedRegions);
    }

    private void updateWatchedTriggers() {
        watchingTriggers = filterSubscribed(track.getTriggers(), subscribedTriggers);
    }

    public void subscribeRegionId(String id) {
        if (subscribedRegions.add(id)) updateWatchedRegions();
    }

    public boolean unsubscribeRegionId(String id) {
        boolean removed = subscribedRegions.remove(id);
        if (removed) updateWatchedRegions();
        return removed;
    }

    public void subscribeTriggerId(String id) {
        if (subscribedTriggers.add(id)) updateWatchedTriggers();
    }

    public boolean unsubscribeTriggerId(String id) {
        boolean removed = subscribedTriggers.remove(id);
        if (removed) updateWatchedTriggers();
        return removed;
    }

    public record TickLocation(
            UUID player,
            long tick,
            Vec pos,
            float yaw
    ) {}
}
