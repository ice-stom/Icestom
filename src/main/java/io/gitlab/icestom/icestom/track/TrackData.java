package io.gitlab.icestom.icestom.track;

import io.gitlab.icestom.icestom.track.checkpoint.Checkpoint;
import net.minestom.server.coordinate.Pos;

import java.util.Map;

public interface TrackData {
    String getId();
    Pos getSpawnLocation();
    Map<Checkpoint, Integer> getCheckpoints();
}