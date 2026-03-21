package io.gitlab.icestom.icestom.race;

import io.gitlab.icestom.icestom.leaderboard.Leaderboard;
import io.gitlab.icestom.icestom.race.ui.RaceLeaderboardRow;

import java.util.function.Function;

public class RaceLeaderboard<Row extends RaceLeaderboardRow> extends Leaderboard<Row, Race.RaceParticipant> {
    private final Race race;

    public RaceLeaderboard(Function<Race.RaceParticipant, Row> newRowFactory, Race race) {
        super(newRowFactory);
        this.race = race;
    }

    public RaceLeaderboardSnapshot<Row> getSnapshot() {
        return new RaceLeaderboardSnapshotRecord<>(
                race.getTrack().getId(),
                race.getTotalLaps(),
                race.getTotalPits(),
                null,
                leaderboard
        );
    }
}
