package io.gitlab.icestom.icestom.timetrial.event;

import io.gitlab.icestom.icestom.timetrial.TimeTrialingInstance;
import io.gitlab.icestom.icestom.track.event.TrackInstanceEvent;
import org.jetbrains.annotations.NotNull;

public interface TimeTrialInstanceEvent extends TrackInstanceEvent {
    @NotNull TimeTrialingInstance getInstance();
}
