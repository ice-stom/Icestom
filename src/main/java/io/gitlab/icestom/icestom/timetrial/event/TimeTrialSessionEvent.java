package io.gitlab.icestom.icestom.timetrial.event;

import io.gitlab.icestom.icestom.timetrial.TimeTrialingInstance;
import net.minestom.server.event.trait.InstanceEvent;
import org.jetbrains.annotations.NotNull;

public interface TimeTrialSessionEvent extends InstanceEvent {
    @NotNull TimeTrialingInstance getInstance();
}
