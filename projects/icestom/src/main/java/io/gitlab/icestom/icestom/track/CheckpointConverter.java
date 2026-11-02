package io.gitlab.icestom.icestom.track;

import io.gitlab.icestom.icestom.track.colliders.CrossCollider;
import io.gitlab.icestom.icestom.track.colliders.types.CuboidCollider;
import io.gitlab.icestom.icestom.track.colliders.types.LineCollider;
import io.gitlab.icestom.icestom.track.colliders.types.PlaneCollider;
import io.gitlab.icestom.stomtrack.TrackCheckpoint;
import org.jspecify.annotations.NonNull;

import java.util.HashMap;
import java.util.Map;

public interface CheckpointConverter {

    private static @NonNull CrossCollider convertCheckpoint(TrackCheckpoint checkpoint) {
        return switch (checkpoint) {
            case TrackCheckpoint.CuboidCheckpoint cuboid -> new CuboidCollider(
                    PositionConverter.fromVec3(cuboid.min()),
                    PositionConverter.fromVec3(cuboid.max())
            );
            case TrackCheckpoint.LineCheckpoint lineCheckpoint -> new LineCollider(
                    PositionConverter.fromVec2(lineCheckpoint.a(), lineCheckpoint.y()),
                    PositionConverter.fromVec2(lineCheckpoint.b(), lineCheckpoint.y()),
                    lineCheckpoint.height()
            );
            case TrackCheckpoint.PlaneCheckpoint planeCheckpoint -> new PlaneCollider(
                    PositionConverter.fromVec3(planeCheckpoint.a()),
                    PositionConverter.fromVec3(planeCheckpoint.b()),
                    PositionConverter.fromVec3(planeCheckpoint.up()),
                    planeCheckpoint.height()
            );
            default -> throw new IllegalStateException("Unexpected value: " + checkpoint);
        };
    }

    static Map<CrossCollider, Integer> fromCheckpointDef(Map<TrackCheckpoint, Integer> defs) {
        Map<CrossCollider, Integer> map = new HashMap<>();

        defs.forEach((trackCheckpoint, integer) -> {
            map.put(
                    convertCheckpoint(trackCheckpoint),
                    integer
            );
        });

        return map;
    }
}
