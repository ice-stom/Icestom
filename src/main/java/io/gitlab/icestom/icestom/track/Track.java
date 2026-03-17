package io.gitlab.icestom.icestom.track;

import io.gitlab.icestom.icestom.IceStom;
import io.gitlab.icestom.icestom.track.checkpoint.Checkpoint;
import io.gitlab.icestom.icestom.track.format.TrackData;
import io.gitlab.icestom.icestom.openboatutils.OpenBoatUtilsPacket;
import net.hollowcube.polar.PolarWorld;
import net.minestom.server.coordinate.Pos;
import org.intellij.lang.annotations.Subst;

import java.util.*;

public class Track implements TrackData {

    private final String id;
    private final Pos spawnLocation;
    private final Map<Checkpoint, Integer> checkpoints;
    private final List<Pos> gridLocations;
    private final List<OpenBoatUtilsPacket> openBoatUtilsPackets;

    private final Map<Integer, List<Checkpoint>> checkpoint_lookup = new HashMap<>();

    private final PolarWorld world;

    public Track(TrackData trackData, PolarWorld world) {
        this.id = trackData.getId();
        this.spawnLocation = trackData.getSpawnLocation();
        this.checkpoints = trackData.getCheckpoints();
        this.gridLocations = trackData.getGridLocations();
        this.openBoatUtilsPackets = trackData.getOpenBoatUtilsPackets();

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
    public @Subst(IceStom.NAMESPACE) String getId() { return id; }

    @Override
    public Pos getSpawnLocation() { return spawnLocation; }

    @Override
    public Map<Checkpoint, Integer> getCheckpoints() { return checkpoints; }

    @Override
    public List<Pos> getGridLocations() {
        return gridLocations;
    }

    @Override
    public List<OpenBoatUtilsPacket> getOpenBoatUtilsPackets() {
        return openBoatUtilsPackets;
    }

    public List<Checkpoint> getCheckpoints(int index) {
        return checkpoint_lookup.get(index);
    }

    public PolarWorld getWorld() { return world; }
}
