package io.gitlab.icestom.tracklua;

import io.gitlab.icestom.stomtrack.TrackFile;
import io.gitlab.icestom.stomtrack.type.Location;

import java.util.List;
import java.util.UUID;

public interface TrackHandle {
    void teleportPlayer(UUID player, Location location);

    List<UUID> getPlayers();

    boolean supportsWorldMutation();

    TrackFile getTrackFile();
    LuaFileSystem getFileSystem();
}
