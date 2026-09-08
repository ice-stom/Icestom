package io.gitlab.icestom.icestom.race.event;

import io.gitlab.icestom.icestom.race.RaceStage;
import org.jetbrains.annotations.NotNull;

public class RaceLeaderboardUpdateEvent implements RaceEvent {

    private final @NotNull RaceStage instance;

    public RaceLeaderboardUpdateEvent(@NotNull RaceStage instance) {
        this.instance = instance;
    }

    @Override
    public @NotNull RaceStage getInstance() {
        return instance;
    }
}
