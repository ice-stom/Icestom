package io.gitlab.icestom.icestom.instance;

import io.gitlab.icestom.icestom.IceStom;
import io.gitlab.icestom.icestom.entity.IceStomPlayer;
import io.gitlab.icestom.icestom.openboatutils.OBUContextPackets;
import io.gitlab.icestom.icestom.openboatutils.GroupedPacketPayload;
import io.gitlab.icestom.icestom.track.Track;
import io.gitlab.icestom.icestom.track.checkpoint.Checkpoint;
import io.gitlab.icestom.icestom.track.checkpoint.PlaneCheckpoint;
import io.gitlab.icestom.icestom.track.checkpoint.TerribleDebugCheckpointDrawer;
import io.gitlab.icestom.icestom.track.checkpoint.TickMovement;
import io.gitlab.icestom.icestom.openboatutils.OBUSettingsPackets;
import net.hollowcube.polar.PolarLoader;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerBlockBreakEvent;
import net.minestom.server.event.player.PlayerBlockPlaceEvent;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.minestom.server.instance.LightingChunk;
import net.minestom.server.inventory.PlayerInventory;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

@SuppressWarnings("UnstableApiUsage")
public abstract class TrackInstance extends BoatInstance implements SpawnLocation {

    private static final Logger log = LoggerFactory.getLogger(TrackInstance.class);
    protected final Track track;
    private final Map<Player, Vec> lastTickPositions = new HashMap<>();

    public TrackInstance(Track track) {
        super(Key.key(IceStom.NAMESPACE, "track/" + track.getWorldId()));

        this.track = track;

        setChunkSupplier(LightingChunk::new);
        setChunkLoader(new PolarLoader(track.getWorld()));

        eventNode().addListener(PlayerBlockBreakEvent.class, event -> event.setCancelled(true));
        eventNode().addListener(PlayerBlockPlaceEvent.class, event -> event.setCancelled(true));
    }

    @Override
    public void tick(long time) {
        super.tick(time);

        for (Checkpoint checkpoint : track.getCheckpoints().keySet()) {
            if (checkpoint instanceof PlaneCheckpoint planeCheckpoint) {
                TerribleDebugCheckpointDrawer.drawPlaneCheckpoint(this, planeCheckpoint);
            }
        }

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

        onPlayerMovements(movementMap);
    }

    @Override
    public void resetPlayer(Player player) {
        Pos spawn = track.getSpawnLocation();

        removeBoat(player);

        if (!track.getOpenBoatUtilsPackets().isEmpty()) {
            if (((IceStomPlayer) player).getOpenBoatUtilsVersion() == null) {
                player.sendMessage(Component.translatable("message.timetrial.requires_open_boat_utils"));
                drop(player);
                IceStom.getInstance().getSpawnInstance().consume(player);
            } else {
                try {
                    List<OBUSettingsPackets> packets = new ArrayList<>();
                    packets.add(new OBUSettingsPackets.ResetPacket());
                    packets.addAll(track.getOpenBoatUtilsPackets());

                    GroupedPacketPayload groupedPacketPayload = new GroupedPacketPayload(packets);

                    player.sendPacket(new OBUSettingsPackets.TransactionPacket(groupedPacketPayload).toPacket(OBUSettingsPackets.getChannel()));
                } catch (IOException _) {}
            }
        }

        createBoat(player, spawn);
    }

    @Override
    public void drop(Player player) {
        removeBoat(player);
        try {
            player.sendPacket(new OBUContextPackets.ResetContext().toPacket(OBUContextPackets.getChannel()));
        } catch (IOException _) {}
    }

    protected abstract void onPlayerMovements(Map<Player, TickMovement> movements);
    protected abstract boolean shouldTrackPlayer(Player player);

    public Track getTrack() {
        return track;
    }
}
