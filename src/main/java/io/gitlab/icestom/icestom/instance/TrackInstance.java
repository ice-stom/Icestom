package io.gitlab.icestom.icestom.instance;

import io.gitlab.icestom.icestom.IceStom;
import io.gitlab.icestom.icestom.entity.IceStomPlayer;
import io.gitlab.icestom.icestom.track.Track;
import io.gitlab.icestom.icestom.track.checkpoint.Checkpoint;
import io.gitlab.icestom.icestom.track.checkpoint.PlaneCheckpoint;
import io.gitlab.icestom.icestom.track.checkpoint.TerribleDebugCheckpointDrawer;
import io.gitlab.icestom.icestom.track.checkpoint.TickMovement;
import io.gitlab.icestom.icestom.openboatutils.OpenBoatUtilsPacket;
import net.hollowcube.polar.PolarLoader;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.LightingChunk;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public abstract class TrackInstance extends BoatInstance implements SpawnLocation {

    protected final Track track;
    private final Map<Player, Vec> last_tick_positions = new HashMap<>();

    public TrackInstance(Track track) {
        super(Key.key(IceStom.NAMESPACE, "track/" + track.getId()));

        this.track = track;

        setChunkLoader(new PolarLoader(track.getWorld()));
        setChunkSupplier(LightingChunk::new);
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
                last_tick_positions.remove(player);
                continue;
            };

            Vec current = player.getPosition().asVec();
            @Nullable Vec last = last_tick_positions.get(player);

            movementMap.put(player, new TickMovement(last, current));

            last_tick_positions.put(player, current);
        }

        onPlayerMovements(movementMap);
    }

    @Override
    public void resetPlayer(Player player) {
        Pos spawn = track.getSpawnLocation();

        removeBoat(player);

        if (!track.getOpenBoatUtilsPackets().isEmpty()) {
            if (((IceStomPlayer) player).getOpenBoatUtilsVersion() == null) {
                drop(player);
                player.sendMessage(Component.text("You need OpenBoatUtils to play this track"));
                IceStom.getInstance().getSpawnInstance().consume(player);
            } else {
                try {
                    player.sendPacket(new OpenBoatUtilsPacket.ResetPacket().toPacket());
                    for (OpenBoatUtilsPacket openBoatUtilsPacket : track.getOpenBoatUtilsPackets()) {
                        player.sendPacket(openBoatUtilsPacket.toPacket());
                    }
                } catch (IOException _) {}
            }
        }

        createBoat(player, spawn);
    }

    @Override
    public void drop(Player player) {
        removeBoat(player);
        try {
            player.sendPacket(new OpenBoatUtilsPacket.ResetPacket().toPacket());
        } catch (IOException _) {}
    }

    protected abstract void onPlayerMovements(Map<Player, TickMovement> movements);
    protected abstract boolean shouldTrackPlayer(Player player);

    public Track getTrack() {
        return track;
    }
}
