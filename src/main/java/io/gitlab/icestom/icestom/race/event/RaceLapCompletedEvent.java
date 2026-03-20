package io.gitlab.icestom.icestom.race.event;

import io.gitlab.icestom.icestom.race.Race;
import io.gitlab.icestom.icestom.timetrial.lap.TimedLapResultSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RaceLapCompletedEvent implements RaceParticipantEvent {

    @NotNull private final Race.RaceParticipant participant;
    @NotNull private final Race race;

    @NotNull private final TimedLapResultSource result;
    @NotNull private final TimedLapResultSource previousBest;

    public RaceLapCompletedEvent(@NotNull Race.RaceParticipant participant, @NotNull Race race, @NotNull TimedLapResultSource result, @NotNull TimedLapResultSource previousBest) {
        this.participant = participant;
        this.race = race;
        this.result = result;
        this.previousBest = previousBest;
    }

    @Override
    public @NotNull Race.RaceParticipant participant() {
        return participant;
    }

    @Override
    public @NotNull Race race() {
        return race;
    }

    public @NotNull TimedLapResultSource getResult() {
        return result;
    }

    public @Nullable TimedLapResultSource getPreviousBestResult() {
        return previousBest;
    }
}
