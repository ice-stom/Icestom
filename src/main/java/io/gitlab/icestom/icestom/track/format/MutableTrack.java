package io.gitlab.icestom.icestom.track.format;

import io.gitlab.icestom.icestom.track.checkpoint.Checkpoint;
import io.gitlab.icestom.icestom.openboatutils.OBUSettingsPackets;
import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.Pos;

import java.util.List;
import java.util.Map;

public class MutableTrack implements TrackData {

    public String id;
    public Component name;
    public boolean looped;
    public Pos spawnLocation;
    public Map<Checkpoint, Integer> checkpoints;
    public List<Pos> gridLocations;
    public List<OBUSettingsPackets> openBoatUtilsPackets;

    public MutableTrack(String id, Component name, boolean looped, Pos spawnLocation, Map<Checkpoint, Integer> checkpoints, List<Pos> gridLocations, List<OBUSettingsPackets> openBoatUtilsPackets) {
        this.id = id;
        this.name = name;
        this.looped = looped;
        this.spawnLocation = spawnLocation;
        this.checkpoints = checkpoints;
        this.gridLocations = gridLocations;
        this.openBoatUtilsPackets = openBoatUtilsPackets;
    }

    public MutableTrack(TrackData track) {
        id = track.getId();
        name = track.getName();
        looped = track.isLooped();
        spawnLocation = track.getSpawnLocation();
        checkpoints = track.getCheckpoints();
        gridLocations = track.getGridLocations();
        openBoatUtilsPackets = track.getOpenBoatUtilsPackets();
    }

    @Override
    public String getId() { return id; }

    @Override
    public Component getName() { return name; }

    @Override
    public boolean isLooped() { return looped; }

    @Override
    public Pos getSpawnLocation() { return spawnLocation; }

    @Override
    public Map<Checkpoint, Integer> getCheckpoints() { return checkpoints; }

    @Override
    public List<Pos> getGridLocations() {
        return gridLocations;
    }

    @Override
    public List<OBUSettingsPackets> getOpenBoatUtilsPackets() {
        return openBoatUtilsPackets;
    }
}
