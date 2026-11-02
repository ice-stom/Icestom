package io.gitlab.icestom.icestom.track.event;

import io.gitlab.icestom.icestom.instance.TrackInstance;
import net.minestom.server.event.trait.InstanceEvent;
import org.jetbrains.annotations.NotNull;

public interface TrackInstanceEvent extends InstanceEvent {
    @NotNull TrackInstance getInstance();
}
