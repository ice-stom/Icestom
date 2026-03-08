package io.gitlab.icestom.icestom.instance;

import io.gitlab.icestom.icestom.IceStom;
import io.gitlab.icestom.icestom.track.Track;
import io.gitlab.icestom.icestom.track.checkpoint.Checkpoint;
import io.gitlab.icestom.icestom.track.checkpoint.PlaneCheckpoint;
import io.gitlab.icestom.icestom.track.checkpoint.TerribleDebugCheckpointDrawer;
import io.gitlab.icestom.icestom.track.checkpoint.TickMovement;
import net.hollowcube.polar.PolarLoader;
import net.kyori.adventure.key.Key;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.LightingChunk;

import java.util.HashMap;
import java.util.Map;

public abstract class TrackInstance extends BoatInstance {

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
            Vec current = player.getPosition().asVec();
            Vec last = last_tick_positions.get(player);

            movementMap.put(player, new TickMovement(last, current));

            last_tick_positions.put(player, current);
        }

        onPlayerMovements(movementMap);
    }

    protected abstract void onPlayerMovements(Map<Player, TickMovement> movements);

    public Track getTrack() {
        return track;
    }
}
