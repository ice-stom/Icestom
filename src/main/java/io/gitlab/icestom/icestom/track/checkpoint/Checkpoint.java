package io.gitlab.icestom.icestom.track.checkpoint;

import io.gitlab.icestom.icestom.track.format.serialization.CheckpointSerializer;
import net.minestom.server.entity.Player;

import java.util.Map;

public interface Checkpoint extends CheckpointSerializer {
    Map<Player, Double> detectCrosses(Map<Player, TickMovement> movements);
}

