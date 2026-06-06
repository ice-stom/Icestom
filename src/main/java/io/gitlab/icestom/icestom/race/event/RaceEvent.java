package io.gitlab.icestom.icestom.race.event;

import io.gitlab.icestom.icestom.race.RaceInstance;
import net.minestom.server.event.trait.InstanceEvent;
import org.jetbrains.annotations.NotNull;

public interface RaceEvent extends InstanceEvent {
    @NotNull RaceInstance getInstance();
}
