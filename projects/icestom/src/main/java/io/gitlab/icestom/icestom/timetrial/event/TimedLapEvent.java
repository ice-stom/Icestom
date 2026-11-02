package io.gitlab.icestom.icestom.timetrial.event;

import io.gitlab.icestom.icestom.timetrial.lap.TimedLap;
import net.minestom.server.event.trait.PlayerEvent;

public interface TimedLapEvent extends PlayerEvent {
    TimedLap getLap();
}
