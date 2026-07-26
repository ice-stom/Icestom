package io.gitlab.icestom.stomtrack;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.openboatutils.protocol.channels.OBUSettingsPacket;
import io.gitlab.icestom.stomtrack.type.Location;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface TrackFile {

    int VERSION = 2;

    @JacksonXmlProperty(isAttribute = true)
    default int getVersion() {
        return VERSION;
    }

    @NotNull String getId();
    @NotNull Component getName();

    boolean isLooped();

    @NotNull Location getSpawnLocation();
    @NotNull Map<String, String> getTags();
    @NotNull Map<TrackCheckpoint, Integer> getCheckpoints();
    @NotNull List<Location> getGrid();
    @NotNull List<OBUSettingsPacket> getOpenBoatUtils();
    @NotNull Map<TrackRegion, Set<String>> getRegions();
    @NotNull Map<TrackTrigger, Set<String>> getTriggers();
    @NotNull Map<String, Location> getLocations();
}
