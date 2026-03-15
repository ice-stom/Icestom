package io.gitlab.icestom.icestom.track.format;

import io.gitlab.icestom.icestom.track.checkpoint.Checkpoint;
import net.minestom.server.coordinate.Pos;

import java.util.List;
import java.util.Map;

public class MutableTrack implements TrackData {

    public String id;
    public Pos spawnLocation;
    public Map<Checkpoint, Integer> checkpoints;
    public List<Pos> gridLocations;

    public MutableTrack(String id, Pos spawnLocation, Map<Checkpoint, Integer> checkpoints, List<Pos> gridLocations) {
        this.id = id;
        this.spawnLocation = spawnLocation;
        this.checkpoints = checkpoints;
        this.gridLocations = gridLocations;
    }

    public MutableTrack(TrackData track) {
        id = track.getId();
        spawnLocation = track.getSpawnLocation();
        checkpoints = track.getCheckpoints();
    }

    @Override
    public String getId() { return id; }

    @Override
    public Pos getSpawnLocation() { return spawnLocation; }

    @Override
    public Map<Checkpoint, Integer> getCheckpoints() { return checkpoints; }

    @Override
    public List<Pos> getGridLocations() {
        return gridLocations;
    }
}
