package io.gitlab.icestom.icestom.track;

import io.gitlab.icestom.icestom.track.colliders.CrossCollider;
import io.gitlab.icestom.icestom.track.colliders.types.LineCollider;
import io.gitlab.icestom.stomtrack.TrackTrigger;
import org.jspecify.annotations.NonNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public interface TriggerConverter {
    private static @NonNull CrossCollider convertTrigger(TrackTrigger region) {
        return switch (region) {
            case TrackTrigger.LineTrigger lineCheckpoint -> new LineCollider(
                    PositionConverter.fromVec2(lineCheckpoint.a(), lineCheckpoint.y()),
                    PositionConverter.fromVec2(lineCheckpoint.b(), lineCheckpoint.y()),
                    lineCheckpoint.height()
            );
            default -> throw new IllegalStateException("Unexpected value: " + region);
        };
    }

    static Map<CrossCollider, Set<String>> fromTriggerDef(Map<TrackTrigger, Set<String>> defs) {
        Map<CrossCollider, Set<String>> map = new HashMap<>();

        defs.forEach((trackRegion, tags) -> {
            map.put(
                    convertTrigger(trackRegion),
                    tags
            );
        });

        return map;
    }
}
