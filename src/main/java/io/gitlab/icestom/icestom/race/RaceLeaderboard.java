package io.gitlab.icestom.icestom.race;

import io.gitlab.icestom.icestom.leaderboard.Leaderboard;

import java.util.function.Function;

public class RaceLeaderboard<Row extends RaceLeaderboardRow> extends Leaderboard<Row, RaceStage.RaceParticipant> {
    private final RaceStage race;

    public RaceLeaderboard(Function<RaceStage.RaceParticipant, Row> newRowFactory, RaceStage raceInstance) {
        super(newRowFactory);
        this.race = raceInstance;
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
