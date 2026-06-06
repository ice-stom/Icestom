package io.gitlab.icestom.icestom.race.event;

import io.gitlab.icestom.icestom.race.RaceInstance;
import net.minestom.server.event.Event;

public interface RaceEvent extends Event {
    RaceInstance race();
}
