package io.gitlab.icestom.stomtrack;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.gitlab.icestom.stomtrack.type.Vec2;
import io.gitlab.icestom.stomtrack.type.Vec3;

import java.util.List;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
@JsonSubTypes({
        @JsonSubTypes.Type(value = TrackTrigger.LineCheckpoint.class, name = "line"),
})
public interface TrackTrigger {
    record LineCheckpoint(Vec2 a, Vec2 b, double y, double height) implements TrackCheckpoint {}
}