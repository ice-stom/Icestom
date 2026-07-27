package io.gitlab.icestom.icestom.track;

import io.gitlab.icestom.stomtrack.type.Location;
import io.gitlab.icestom.stomtrack.type.Vec2;
import io.gitlab.icestom.stomtrack.type.Vec3;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;

public interface PositionConverter {
    static Pos fromLocation(Location location) {
        return new Pos(
                location.x(),
                location.y(),
                location.z(),
                location.yaw(),
                location.pitch()
        );
    }

    static Vec fromVec3(Vec3 vec3) {
        return new Vec(vec3.x(), vec3.y(), vec3.z());
    }

    static Vec fromVec2(Vec2 vec2, double y) {
        return new Vec(vec2.x(), y, vec2.y());
    }
}
