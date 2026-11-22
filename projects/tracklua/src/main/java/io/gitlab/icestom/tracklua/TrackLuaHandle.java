package io.gitlab.icestom.tracklua;

import io.gitlab.icestom.stomtrack.TrackFile;
import io.gitlab.icestom.stomtrack.type.Location;

import java.util.List;
import java.util.UUID;

public interface TrackLuaHandle {
    void teleportPlayer(UUID player, Location location);

    List<UUID> getPlayers();

    WorldLuaHandle getWorld();

    void subscribeTrigger(String tag);
    void subscribeRegion(String tag);
    void unsubscribeTrigger(String tag);
    void unsubscribeRegion(String tagT);

    TrackFile getTrackFile();
    LuaFileSystem getFileSystem();
}
