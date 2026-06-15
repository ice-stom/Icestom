package io.gitlab.icestom.icestom.race.event;

import io.gitlab.icestom.icestom.race.RaceInstance;
import io.gitlab.icestom.icestom.timetrial.event.TimedLapEvent;
import io.gitlab.icestom.icestom.timetrial.lap.TimedLap;
import org.jetbrains.annotations.NotNull;

public class RaceLapTimerEvent implements RaceEvent, TimedLapEvent {
    @NotNull private final RaceInstance instance;
    @NotNull private final RaceInstance.RaceParticipant participant;
    @NotNull private final TimedLap lap;
    public RaceLapTimerEvent(@NotNull RaceInstance instance, @NotNull RaceInstance.RaceParticipant participant) {
        this.instance = instance;
        this.participant = participant;
        this.lap = participant.getCurrentLap();
    }

    @Override
    public @NotNull TimedLap getLap() {
        return lap;
    }

    @Override
    public @NotNull RaceInstance.RaceParticipant getParticipant() {
        return participant;
    }

    @Override
    public @NotNull RaceInstance getInstance() { return instance; }
}

