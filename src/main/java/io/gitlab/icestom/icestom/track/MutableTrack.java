package io.gitlab.icestom.icestom.track;

import io.gitlab.icestom.icestom.track.checkpoint.Checkpoint;
import net.minestom.server.coordinate.Pos;

import java.util.Map;

public class MutableTrack implements TrackData {

    private String id;
    private Pos spawnLocation;
    private Map<Checkpoint, Integer> checkpoints;

    public MutableTrack(String id, Pos spawnLocation, Map<Checkpoint, Integer> checkpoints) {
        this.id = id;
        this.spawnLocation = spawnLocation;
        this.checkpoints = checkpoints;
    }

    public MutableTrack() {}

    @Override
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    @Override
    public Pos getSpawnLocation() { return spawnLocation; }
    public void setSpawnLocation(Pos spawnLocation) { this.spawnLocation = spawnLocation; }

    @Override
    public Map<Checkpoint, Integer> getCheckpoints() { return checkpoints; }
    public void setCheckpoints(Map<Checkpoint, Integer> checkpoints) { this.checkpoints = checkpoints; }
}
