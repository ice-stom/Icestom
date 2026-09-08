package io.gitlab.icestom.icestom.track;

import io.gitlab.icestom.icestom.IceStom;
import io.gitlab.icestom.icestom.instance.TrackInstance;
import io.gitlab.icestom.icestom.timetrial.lap.TimedLap;
import io.gitlab.icestom.icestom.track.colliders.CrossCollider;
import io.github.openboatutils.protocol.channels.OBUSettingsPacket;
import io.gitlab.icestom.icestom.track.colliders.InsideCollider;
import io.gitlab.icestom.stomtrack.EnvironmentFile;
import io.gitlab.icestom.stomtrack.TrackFile;
import net.hollowcube.polar.PolarWorld;
import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import org.intellij.lang.annotations.Subst;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.*;

public class Track {

    private final String id;
    private final String environmentId;
    private final Component name;
    private final boolean looped;
    private final Pos spawnLocation;
    private final Map<CrossCollider, Integer> checkpoints;
    private final List<Pos> gridLocations;
    private final Map<String, String> tags;
    private final List<OBUSettingsPacket> openBoatUtilsPackets;
    private final Map<InsideCollider, Set<String>> regions;
    private final Map<CrossCollider, Set<String>> triggers;
    private final Map<String, Pos> locations;
    private final EnvironmentFile trackEnvironmentData;

    private final Map<Integer, List<CrossCollider>> checkpoint_lookup = new HashMap<>();

    private final PolarWorld world;

    private final int wrapIndex;

    public Track(TrackFile trackFile, PolarWorld world, EnvironmentFile environmentData, @Nullable String environmentId) {
        this.id = trackFile.getId();
        this.environmentId = environmentId;
        this.name = trackFile.getName();
        this.looped = trackFile.isLooped();
        this.tags = trackFile.getTags();
        this.spawnLocation = PositionConverter.fromLocation(trackFile.getSpawnLocation());
        this.checkpoints = CheckpointConverter.fromCheckpointDef(trackFile.getCheckpoints());
        this.gridLocations = trackFile.getGrid().stream().map(PositionConverter::fromLocation).toList();
        this.openBoatUtilsPackets = trackFile.getOpenBoatUtils();
        this.regions = RegionConverter.fromRegionDef(trackFile.getRegions());
        this.triggers = TriggerConverter.fromTriggerDef(trackFile.getTriggers());
        this.locations = LocationConverter.fromLocationDef(trackFile.getLocations());
        this.trackEnvironmentData = environmentData;

        this.world = world;

        for (Map.Entry<CrossCollider, Integer> entry : checkpoints.entrySet()) {
            CrossCollider checkpoint = entry.getKey();
            int index = entry.getValue();

            List<CrossCollider> checkpoints = checkpoint_lookup.computeIfAbsent(index, _ -> new ArrayList<>());

            checkpoints.add(checkpoint);
        }

        wrapIndex = checkpoints.values().stream().max(Integer::compareTo).orElse(-1) + 1;
    }

    public int wrapCheckpointIndex(int checkpoint) {
        if (!looped) return checkpoint % (wrapIndex + 1);

        return checkpoint % wrapIndex;
    }

    public boolean isLastCheckpoint(int checkpoint) {
        if (!looped) return checkpoint == (wrapIndex - 1);

        return checkpoint % wrapIndex == 0;
    }

    public @Subst(IceStom.NAMESPACE) @NonNull String getId() { return id; }

    public @NonNull Component getName() { return name; }

    public @NonNull String getEnvironmentId() {
        if (environmentId != null) {
            return environmentId;
        }

        return id;
    }

    public boolean isLooped() { return looped; }

    public @NonNull Pos getSpawnLocation() { return spawnLocation; }

    public @NonNull Map<CrossCollider, Integer> getCheckpoints() { return checkpoints; }

    public @NonNull List<Pos> getGridLocations() {
        return gridLocations;
    }

    public @NonNull List<OBUSettingsPacket> getOpenBoatUtilsPackets() {
        return openBoatUtilsPackets;
    }

    public @NotNull EnvironmentFile getEnvironmentData() { return trackEnvironmentData; }

    public List<CrossCollider> getCheckpoints(int index) {
        return checkpoint_lookup.get(index);
    }

    public Map<String, String> getTags() { return tags; }

    public Map<InsideCollider, Set<String>> getRegions() { return regions; }

    public Map<CrossCollider, Set<String>> getTriggers() { return triggers; }

    public Map<String, Pos> getLocations() { return locations; }

    public PolarWorld getWorld() { return world; }
}
