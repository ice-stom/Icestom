package io.gitlab.icestom.icestom.race.event;

import io.gitlab.icestom.icestom.race.RaceInstance;
import io.gitlab.icestom.icestom.timetrial.lap.TimedLapResultSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RaceLeaderboardUpdateEvent implements RaceInstanceEvent {

    @NotNull private final RaceInstance race;

    public RaceLeaderboardUpdateEvent(@NotNull RaceInstance raceInstance) {
        this.race = raceInstance;
    }

    @Override
    public @NotNull RaceInstance getInstance() {
        return race;
    }
}
