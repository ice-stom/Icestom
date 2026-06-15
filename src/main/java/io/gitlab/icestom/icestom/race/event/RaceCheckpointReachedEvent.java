package io.gitlab.icestom.icestom.race.event;

import io.gitlab.icestom.icestom.race.RaceInstance;
import io.gitlab.icestom.icestom.timetrial.lap.TimedLap;
import org.jetbrains.annotations.NotNull;

public class RaceCheckpointReachedEvent implements RaceEvent {
    private final @NotNull RaceInstance.RaceParticipant participant;
    private final @NotNull RaceInstance instance;
    private final @NotNull TimedLap lap;
    private final int checkpointIndex;

    public RaceCheckpointReachedEvent(@NotNull RaceInstance.RaceParticipant participant, @NotNull RaceInstance instance, @NotNull TimedLap lap, int checkpointIndex) {
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

    public int getCheckpointIndex() {
        return checkpointIndex;
    }
}
