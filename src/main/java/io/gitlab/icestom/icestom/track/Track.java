package io.gitlab.icestom.icestom.track;

import io.gitlab.icestom.icestom.IceStom;
import io.gitlab.icestom.icestom.track.checkpoint.Checkpoint;
import net.hollowcube.polar.PolarWorld;
import net.minestom.server.coordinate.Pos;
import org.intellij.lang.annotations.Subst;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;

public class Track implements TrackData {

    private final String id;
    private final Pos spawnLocation;
    private final Map<Checkpoint, Integer> checkpoints;

    private final PolarWorld world;

    public Track(TrackData trackData, PolarWorld world) {
        this.id = trackData.getId();
        this.spawnLocation = trackData.getSpawnLocation();
        this.checkpoints = trackData.getCheckpoints();

        this.world = world;
    }

    @Override
    public @Subst(IceStom.NAMESPACE) String getId() { return id; }

    @Override
    public Pos getSpawnLocation() { return spawnLocation; }

    @Override
    public Map<Checkpoint, Integer> getCheckpoints() { return checkpoints; }

    public PolarWorld getWorld() { return world; }

    public @Nullable Integer getCheckpointNumber(@NotNull Checkpoint checkpoint) {
        return checkpoints.get(checkpoint);
    }
}
