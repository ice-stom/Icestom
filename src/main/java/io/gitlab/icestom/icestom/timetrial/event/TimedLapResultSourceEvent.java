package io.gitlab.icestom.icestom.timetrial.event;

import io.gitlab.icestom.icestom.timetrial.lap.TimedLapResultSource;

public interface TimedLapResultSourceEvent extends TimedLapEvent {
    TimedLapResultSource getResult();
}
