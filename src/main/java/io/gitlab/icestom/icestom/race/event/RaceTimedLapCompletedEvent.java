package io.gitlab.icestom.icestom.race.event;

import io.gitlab.icestom.icestom.event.EventParticipant;
import io.gitlab.icestom.icestom.race.RaceStage;
import io.gitlab.icestom.icestom.timetrial.event.TimedLapResultSourceEvent;
import io.gitlab.icestom.icestom.timetrial.lap.TimedLap;
import io.gitlab.icestom.icestom.timetrial.lap.TimedLapResult;
import io.gitlab.icestom.icestom.timetrial.lap.TimedLapResultSource;
import org.jetbrains.annotations.NotNull;

public class RaceTimedLapCompletedEvent implements RaceEvent, TimedLapResultSourceEvent, RaceParticipantEvent {

    private final EventParticipant eventParticipant;
    private final TimedLap lap;
    private final RaceStage instance;

    public RaceTimedLapCompletedEvent(EventParticipant eventParticipant, TimedLap timedLap, RaceStage instance) {
        this.eventParticipant = eventParticipant;
        this.lap = timedLap;
        this.instance = instance;
    }

    @Override
    public EventParticipant getParticipant() {
        return eventParticipant;
    }

    @Override
    public TimedLap getLap() { return lap; }

    @Override
    public @NotNull RaceStage getInstance() {
        return instance;
    }

    @Override
    public TimedLapResultSource getResult() {
        return TimedLapResult.freeze(lap);
    }
}
