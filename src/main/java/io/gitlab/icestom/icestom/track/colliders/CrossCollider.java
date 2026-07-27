package io.gitlab.icestom.icestom.track.colliders;

import io.gitlab.icestom.icestom.track.TickMovement;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public interface CrossCollider {
    default <T> Map<T, Long> detectCrosses(Map<T, TickMovement> movements) {
        Map<T, Long> deltas = new HashMap<>();

        for (Map.Entry<T, TickMovement> entry : movements.entrySet()) {
            @Nullable Long tick_delta = detectCross(entry.getValue());

            if (tick_delta != null) {
                deltas.put(entry.getKey(), tick_delta);
            }
        }

        return deltas;
    }

    @Nullable Long detectCross(TickMovement movement);
}

