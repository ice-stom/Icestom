package io.gitlab.icestom.stomtrack.impl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.openboatutils.protocol.channels.OBUSettingsPacket;
import io.gitlab.icestom.stomtrack.TrackLoader;
import io.gitlab.icestom.stomtrack.TrackRegion;
import io.gitlab.icestom.stomtrack.serde.*;
import io.gitlab.icestom.stomtrack.type.Location;
import io.gitlab.icestom.stomtrack.TrackCheckpoint;
import io.gitlab.icestom.stomtrack.TrackFile;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.*;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonRootName("Track")
public class MutableTrackFile implements TrackFile {

    @JacksonXmlProperty(isAttribute = true)
    private int version = TrackFile.VERSION;

    @JacksonXmlProperty(isAttribute = true)
    @NotNull private String id;
    @NotNull private Component name;
    private boolean looped;
    @NotNull private Location spawnLocation;

    @JsonSerialize(using = CheckpointMapSerde.Serializer.class)
    @JsonDeserialize(using = CheckpointMapSerde.Deserializer.class)
    @NotNull private Map<TrackCheckpoint, Integer> checkpoints;

    @JacksonXmlElementWrapper(localName = "grid")
    @JacksonXmlProperty(localName = "location")
    @NotNull private List<Location> grid;

    @NotNull private Map<String, String> tags;

    @JacksonXmlElementWrapper(localName = "openBoatUtils")
    @JacksonXmlProperty(localName = "packet")
    @NotNull private List<OBUSettingsPacket> openBoatUtils;

    @JsonSerialize(using = TaggedRegionMapDecoder.Serializer.class)
    @JsonDeserialize(using = TaggedRegionMapDecoder.Deserializer.class)
    @NotNull private Map<TrackRegion, Set<String>> regions;

    @JsonSerialize(using = TaggedCheckpointMapDecoder.Serializer.class)
    @JsonDeserialize(using = TaggedCheckpointMapDecoder.Deserializer.class)
    @NotNull private Map<TrackCheckpoint, Set<String>> triggers;

    @NotNull private Map<String, Location> locations;

    public MutableTrackFile() {}

    public MutableTrackFile(@NotNull String id, @NotNull Component name, boolean looped, @NotNull Location spawnLocation) {
        this.id = id;
        this.name = name;
        this.looped = looped;
        this.spawnLocation = spawnLocation;
        this.tags = new HashMap<>();
        this.checkpoints = new HashMap<>();
        this.grid = new ArrayList<>();
        this.openBoatUtils = new ArrayList<>();
        this.regions = new HashMap<>();
        this.triggers = new HashMap<>();
        this.locations = new HashMap<>();
    }

    public MutableTrackFile(TrackFile track) {
        id = track.getId();
        name = track.getName();
        looped = track.isLooped();
        spawnLocation = track.getSpawnLocation();
        tags = track.getTags();
        checkpoints = track.getCheckpoints();
        grid = track.getGrid();
        openBoatUtils = track.getOpenBoatUtils();
        regions = track.getRegions();
        triggers = track.getTriggers();
        locations = track.getLocations();
    }

    public void setId(@NotNull String id) { this.id = id; }

    public void setName(@NotNull Component name) { this.name = name; }
    public void setLooped(boolean looped) { this.looped = looped; }
    public void setSpawnLocation(@NotNull Location spawnLocation) { this.spawnLocation = spawnLocation; }
    public void setCheckpoints(@NotNull Map<TrackCheckpoint, Integer> checkpoints) { this.checkpoints = checkpoints; }
    public void setGrid(@NotNull List<Location> grid) { this.grid = grid;}
    public void setOpenBoatUtils(@NotNull List<OBUSettingsPacket> openBoatUtils) { this.openBoatUtils = openBoatUtils; }
    public void setRegions(@NotNull Map<TrackRegion, Set<String>> regions) { this.regions = regions; }
    public void setTriggers(@NotNull Map<TrackCheckpoint, Set<String>> triggers) { this.triggers = triggers; }

    public void setVersion(int version) { this.version = version; }

    @Override public @NonNull String getId() { return id; }
    @Override public @NonNull Component getName() { return name; }
    @Override public boolean isLooped() { return looped; }
    @Override public @NonNull Location getSpawnLocation() { return spawnLocation; }
    @Override public @NotNull Map<String, String> getTags() { return tags; }
    @Override public @NonNull Map<TrackCheckpoint, Integer> getCheckpoints() { return checkpoints; }
    @Override public @NonNull List<Location> getGrid() {
        return grid;
    }
    @Override public @NonNull List<OBUSettingsPacket> getOpenBoatUtils() {
        return openBoatUtils;
    }
    @Override public @NotNull Map<TrackRegion, Set<String>> getRegions() { return regions; }
    @Override public @NotNull Map<TrackCheckpoint, Set<String>> getTriggers() { return triggers; }
    @Override public @NotNull Map<String, Location> getLocations() { return locations; }

    @Override public int getVersion() { return version; }
}