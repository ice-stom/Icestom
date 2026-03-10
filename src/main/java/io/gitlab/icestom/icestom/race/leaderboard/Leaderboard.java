package io.gitlab.icestom.icestom.race.leaderboard;

import io.gitlab.icestom.icestom.race.Race;
import io.gitlab.icestom.icestom.ui.leaderboard.RaceLeaderboardRow;

import java.util.List;
import java.util.UUID;

public class Leaderboard {

    private final Race race;

    public Leaderboard(Race race) {
        this.race = race;
    }

    public RaceLeaderboardSnapshot getSnapshot() {
        return new RaceLeaderboardSnapshotRecord(
        "something",
                race.getTotalLaps(),
                race.getTotalPits(),
                null,
                List.of(
                        new RaceLeaderboardRow(
                                UUID.randomUUID(),
                                0,
                                null,
                                0,
                                0,
                                false,
                                false
                        )
                )
        );
    }
}
