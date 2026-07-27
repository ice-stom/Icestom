package io.gitlab.icestom.icestom.track;

import io.gitlab.icestom.icestom.track.colliders.InsideCollider;
import io.gitlab.icestom.icestom.track.colliders.types.CuboidCollider;
import io.gitlab.icestom.icestom.track.colliders.types.InvertedCuboidCollider;
import io.gitlab.icestom.stomtrack.TrackRegion;
import org.jspecify.annotations.NonNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public interface RegionConverter {
    private static @NonNull InsideCollider convertRegion(TrackRegion region) {
        return switch (region) {
            case TrackRegion.CuboidRegion cuboid -> new CuboidCollider(
                    PositionConverter.fromVec3(cuboid.min()),
                    PositionConverter.fromVec3(cuboid.max())
            );
            case TrackRegion.InvertedCuboidRegion cuboid -> new InvertedCuboidCollider(
                    PositionConverter.fromVec3(cuboid.min()),
                    PositionConverter.fromVec3(cuboid.max())
            );
            default -> throw new IllegalStateException("Unexpected value: " + region);
        };
    }

    static Map<InsideCollider, Set<String>> fromRegionDef(Map<TrackRegion, Set<String>> defs) {
        Map<InsideCollider, Set<String>> map = new HashMap<>();

        defs.forEach((trackRegion, tags) -> {
            map.put(
                    convertRegion(trackRegion),
                    tags
            );
        });

        return map;
    }
}
