package io.gitlab.icestom.icestom.race;

import io.gitlab.icestom.icestom.leaderboard.Leaderboard;

import java.util.function.Function;

public class RaceLeaderboard<Row extends RaceLeaderboardRow> extends Leaderboard<Row, RaceInstance.RaceParticipant> {
    private final RaceInstance race;

    public RaceLeaderboard(Function<RaceInstance.RaceParticipant, Row> newRowFactory, RaceInstance raceInstance) {
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
