package io.gitlab.icestom.icestom.race.event;

import io.gitlab.icestom.icestom.race.Race;
import io.gitlab.icestom.icestom.timetrial.lap.TimedLap;
import org.jetbrains.annotations.NotNull;

public record RaceCheckpointReachedEvent(@NotNull Race.RaceParticipant participant, @NotNull Race race,
                                         @NotNull TimedLap timedLap,
                                         int checkpointIndex) implements RaceParticipantEvent {

}
