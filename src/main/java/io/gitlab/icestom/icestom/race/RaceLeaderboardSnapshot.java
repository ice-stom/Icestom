package io.gitlab.icestom.icestom.race;

import io.gitlab.icestom.icestom.race.scoreboard.RaceScoreboardRow;

import java.util.List;
import java.util.UUID;

public interface RaceLeaderboardSnapshot {

    String getTrackName();

    int getTotalLaps();
    int getTotalPits();

    UUID getFlapHolder();

    List<RaceScoreboardRow> getRows();

    int getPosition(UUID player);

    default long getDelta(RaceScoreboardRow self, RaceScoreboardRow other) {
        return self.getDelta() - other.getDelta();
    }
}
