package io.gitlab.icestom.icestom.race.event;

import io.gitlab.icestom.icestom.event.EventParticipant;
import io.gitlab.icestom.icestom.event.event.EventParticipantEvent;
import io.gitlab.icestom.icestom.race.RaceStage;
import io.gitlab.icestom.icestom.timetrial.event.TimedLapEvent;
import io.gitlab.icestom.icestom.timetrial.lap.TimedLap;
import org.jetbrains.annotations.NotNull;

public class RaceLapTimerEvent implements RaceEvent, TimedLapEvent, RaceParticipantEvent {

    private final EventParticipant eventParticipant;
    private final TimedLap lap;
    private final RaceStage instance;

    public RaceLapTimerEvent(EventParticipant eventParticipant, TimedLap timedLap, RaceStage instance) {
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
}
