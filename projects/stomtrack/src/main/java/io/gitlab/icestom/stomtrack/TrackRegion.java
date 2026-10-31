package io.gitlab.icestom.stomtrack;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.gitlab.icestom.stomtrack.type.Vec2;
import io.gitlab.icestom.stomtrack.type.Vec3;

import java.util.List;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
@JsonSubTypes({
        @JsonSubTypes.Type(value = TrackRegion.CuboidRegion.class, name = "cuboid"),
        @JsonSubTypes.Type(value = TrackRegion.InvertedCuboidRegion.class, name = "inverted_cuboid"),
        @JsonSubTypes.Type(value = TrackRegion.PolyRegion.class, name = "poly"),
})
public interface TrackRegion {
    record CuboidRegion(Vec3 min, Vec3 max) implements TrackRegion {}
    record InvertedCuboidRegion(Vec3 min, Vec3 max) implements TrackRegion {}
    record PolyRegion(List<Vec2> vec2s, double yMin, double yMax) implements TrackRegion {}
}