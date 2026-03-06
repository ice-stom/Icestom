package io.gitlab.icestom.icestom.instance;

import io.gitlab.icestom.icestom.IceStom;
import io.gitlab.icestom.icestom.track.Track;
import io.gitlab.icestom.icestom.track.checkpoint.Checkpoint;
import io.gitlab.icestom.icestom.track.checkpoint.PlaneCheckpoint;
import io.gitlab.icestom.icestom.track.checkpoint.TerribleDebugCheckpointDrawer;
import io.gitlab.icestom.icestom.track.checkpoint.TickMovement;
import net.hollowcube.polar.PolarLoader;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.event.instance.InstanceTickEvent;
import net.minestom.server.instance.LightingChunk;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("UnstableApiUsage")
public abstract class TrackInstance extends BoatInstance {

    protected final Track track;
    private final Map<Player, Vec> last_tick_positions = new HashMap<>();

    protected int tick = 0;

    public TrackInstance(Track track) {
        super(Key.key(IceStom.NAMESPACE, "track/" + track.getId()));

        this.track = track;

        setChunkLoader(new PolarLoader(track.getWorld()));
        setChunkSupplier(LightingChunk::new);

        eventNode().addListener(InstanceTickEvent.class, this::tick);
    }

    protected void tick(InstanceTickEvent event) {
        Map<Player, TickMovement> movementSet = new HashMap<>();

        for (Checkpoint checkpoint : track.getCheckpoints().keySet()) {
            if (checkpoint instanceof PlaneCheckpoint planeCheckpoint) {
                TerribleDebugCheckpointDrawer.drawPlaneCheckpoint(this, planeCheckpoint);
            }
        }

        for (Player player : getPlayers()) {
            Vec current = player.getPosition().asVec();
            Vec last = last_tick_positions.get(player);

            movementSet.put(player, new TickMovement(last, current));

            last_tick_positions.put(player, current);
        }

        for (Checkpoint checkpoint : track.getCheckpoints().keySet()) {
            checkpoint.detectCrosses(movementSet).forEach((tickMovement, aDouble) -> {
                for (Audience audience : audiences()) {
                    audience.sendMessage(Component.text(String.format("Cross %s: %.2f", track.getCheckpointNumber(checkpoint), aDouble)));
                }
            });
        }

        tick++;
    }

    public int getTick() {
        return tick;
    }
}
