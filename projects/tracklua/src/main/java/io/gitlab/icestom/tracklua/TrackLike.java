package io.gitlab.icestom.tracklua;

import io.gitlab.icestom.stomtrack.TrackFile;
import io.gitlab.icestom.stomtrack.type.Location;

import java.util.UUID;

public interface TrackLike {
    void teleportPlayer(UUID player, Location location);

    TrackFile getTrackFile();
}
