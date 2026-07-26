package io.gitlab.icestom.stomtrack;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.gitlab.icestom.stomtrack.type.Vec2;
import io.gitlab.icestom.stomtrack.type.Vec3;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
@JsonSubTypes({
        @JsonSubTypes.Type(value = TrackCheckpoint.CuboidCheckpoint.class, name = "cuboid"),
        @JsonSubTypes.Type(value = TrackCheckpoint.PlaneCheckpoint.class, name = "plane"),
        @JsonSubTypes.Type(value = TrackCheckpoint.LineCheckpoint.class, name = "line")
})
public interface TrackCheckpoint {
    record CuboidCheckpoint(Vec3 min, Vec3 max) implements TrackCheckpoint {}

    record PlaneCheckpoint(Vec3 a, Vec3 b, Vec3 up, double height) implements TrackCheckpoint {}

    record LineCheckpoint(Vec2 a, Vec2 b, double y, double height) implements TrackCheckpoint {}
}