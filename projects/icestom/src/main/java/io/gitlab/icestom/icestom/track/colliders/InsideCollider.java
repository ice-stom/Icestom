package io.gitlab.icestom.icestom.track.colliders;

import io.gitlab.icestom.icestom.track.PlayerMovement;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public interface InsideCollider {

    default <T> Set<T> detectInside(Map<T, PlayerMovement> movements) {
        Set<T> inside = new HashSet<>();

        for (Map.Entry<T, PlayerMovement> entry : movements.entrySet()) {
            if (detectInside(entry.getValue())) {
                inside.add(entry.getKey());
            }
        }

        return inside;
    }

    boolean detectInside(PlayerMovement movement);
}
