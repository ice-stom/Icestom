package io.gitlab.icestom.icestom.race.event;

import io.gitlab.icestom.icestom.race.RaceInstance;
import io.gitlab.icestom.icestom.timetrial.lap.TimedLapResultSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RaceLapCompletedEvent implements RaceEvent {

    @NotNull private final RaceInstance.RaceParticipant participant;
    @NotNull private final RaceInstance race;
    @NotNull private final TimedLapResultSource result;
    @NotNull private final TimedLapResultSource previousBest;

    public RaceLapCompletedEvent(@NotNull RaceInstance.RaceParticipant participant, @NotNull RaceInstance raceInstance, @NotNull TimedLapResultSource result, @NotNull TimedLapResultSource previousBest) {
        this.participant = participant;
        this.race = raceInstance;
        this.result = result;
        this.previousBest = previousBest;
    }

    @Override
    public @NotNull RaceInstance.RaceParticipant getParticipant() {
        return participant;
    }

    @Override
    public @NotNull RaceInstance getInstance() {
        return race;
    }

    public @NotNull TimedLapResultSource getResult() {
        return result;
    }

    public @Nullable TimedLapResultSource getPreviousBestResult() {
        return previousBest;
    }
}
