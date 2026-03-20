package io.gitlab.icestom.icestom.race.event;

import io.gitlab.icestom.icestom.race.Race;
import net.minestom.server.event.Event;

public interface RaceEvent extends Event {
    Race race();
}
