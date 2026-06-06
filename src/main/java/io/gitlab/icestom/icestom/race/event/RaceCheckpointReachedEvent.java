package io.gitlab.icestom.icestom.race.event;

import io.gitlab.icestom.icestom.race.RaceInstance;
import io.gitlab.icestom.icestom.timetrial.lap.TimedLap;
import org.jetbrains.annotations.NotNull;

public record RaceCheckpointReachedEvent(
        @NotNull RaceInstance.RaceParticipant participant, @NotNull RaceInstance race,
        @NotNull TimedLap timedLap,
        int checkpointIndex
) implements RaceParticipantEvent {}
