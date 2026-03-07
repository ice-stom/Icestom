package io.gitlab.icestom.icestom.track.format.serialization;

import java.util.Map;

public interface CheckpointSerializer {
    Map<String, Object> serialize();
}
