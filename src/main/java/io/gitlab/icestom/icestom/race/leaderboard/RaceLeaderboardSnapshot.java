package io.gitlab.icestom.icestom.race.leaderboard;

import io.gitlab.icestom.icestom.ui.leaderboard.RaceLeaderboardRow;

import java.util.List;
import java.util.UUID;

public interface RaceLeaderboardSnapshot {

    String getTrackName();

    int getTotalLaps();
    int getTotalPits();

    UUID getFlapHolder();

    List<RaceLeaderboardRow> getRows();

    default long getDelta(RaceLeaderboardRow self, RaceLeaderboardRow other) {
        return self.delta() - other.delta();
    }
}
