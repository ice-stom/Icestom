package io.gitlab.icestom.icestom.track;

import io.gitlab.icestom.icestom.IceStom;
import io.gitlab.icestom.icestom.track.checkpoint.Checkpoint;
import io.gitlab.icestom.icestom.track.format.TrackData;
import io.gitlab.icestom.icestom.openboatutils.OBUSettingsPackets;
import io.gitlab.icestom.icestom.track.format.TrackEnvironmentData;
import net.hollowcube.polar.PolarWorld;
import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.Pos;
import org.intellij.lang.annotations.Subst;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.*;

public class Track implements TrackData {

    private final String id;
    private final String worldId;
    private final Component name;
    private final boolean looped;
    private final Pos spawnLocation;
    private final Map<Checkpoint, Integer> checkpoints;
    private final List<Pos> gridLocations;
    private final List<OBUSettingsPackets> openBoatUtilsPackets;
    private final TrackEnvironmentData trackEnvironmentData;

    private final Map<Integer, List<Checkpoint>> checkpoint_lookup = new HashMap<>();

    private final PolarWorld world;

    public Track(TrackData trackData, PolarWorld world, TrackEnvironmentData environmentData, @Nullable String worldId) {
        this.id = trackData.getId();
        this.worldId = worldId;
        this.name = trackData.getName();
        this.looped = trackData.isLooped();
        this.spawnLocation = trackData.getSpawnLocation();
        this.checkpoints = trackData.getCheckpoints();
        this.gridLocations = trackData.getGridLocations();
        this.openBoatUtilsPackets = trackData.getOpenBoatUtilsPackets();
        this.trackEnvironmentData = environmentData;

        this.world = world;

        for (Map.Entry<Checkpoint, Integer> entry : checkpoints.entrySet()) {
            Checkpoint checkpoint = entry.getKey();
            int index = entry.getValue();

            List<Checkpoint> checkpoints = checkpoint_lookup.computeIfAbsent(index, _ -> new ArrayList<>());

            checkpoints.add(checkpoint);
        }
    }

    public int wrapCheckpointIndex(int checkpoint) {
        return checkpoint % checkpoints.size();
    }

    @Override
    public @Subst(IceStom.NAMESPACE) @NonNull String getId() { return id; }

    @Override
    public @NonNull Component getName() { return name; }

    @Override
    public @NonNull String getWorldId() {
        if (worldId != null) {
            return worldId;
        }

        return id;
    }

    @Override
    public boolean isLooped() { return looped; }

    @Override
    public @NonNull Pos getSpawnLocation() { return spawnLocation; }

    @Override
    public @NonNull Map<Checkpoint, Integer> getCheckpoints() { return checkpoints; }

    @Override
    public @NonNull List<Pos> getGridLocations() {
        return gridLocations;
    }

    @Override
    public @NonNull List<OBUSettingsPackets> getOpenBoatUtilsPackets() {
        return openBoatUtilsPackets;
    }

    public @NotNull TrackEnvironmentData getEnvironmentData() { return trackEnvironmentData; }

    public List<Checkpoint> getCheckpoints(int index) {
        return checkpoint_lookup.get(index);
    }

    public PolarWorld getWorld() { return world; }
}
