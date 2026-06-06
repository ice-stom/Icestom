package io.gitlab.icestom.icestom.race.event;

import io.gitlab.icestom.icestom.race.RaceInstance;
import io.gitlab.icestom.icestom.timetrial.lap.TimedLap;
import org.jetbrains.annotations.NotNull;

public class RaceCheckpointReachedEvent implements RaceParticipantEvent {
    private @NotNull RaceInstance.RaceParticipant participant;
    private @NotNull RaceInstance instance;
    private @NotNull TimedLap lap;
    private int checkpointIndex;

    public RaceCheckpointReachedEvent(RaceInstance.RaceParticipant participant, RaceInstance instance, TimedLap lap, int checkpointIndex) {
        this.participant = participant;
        this.instance = instance;
        this.lap = lap;
        this.checkpointIndex = checkpointIndex;
    }

    @Override
    public @NotNull RaceInstance.RaceParticipant getParticipant() {
        return participant;
    }

    @Override
    public @NotNull RaceInstance getInstance() {
        return instance;
    }

    public @NotNull TimedLap getLap() {
        return lap;
    }
}
