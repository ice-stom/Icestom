package io.gitlab.icestom.icestom.race;

import io.gitlab.icestom.icestom.leaderboard.LeaderboardSnapshot;
import io.gitlab.icestom.icestom.race.ui.RaceLeaderboardRow;

public interface RaceLeaderboardSnapshot<R> extends LeaderboardSnapshot<R, Race.RaceParticipant> {

    String getTrackName();

    int getTotalLaps();
    int getTotalPits();

    Race.RaceParticipant getFlapHolder();

    default long getDelta(RaceLeaderboardRow self, RaceLeaderboardRow other) {
        return self.getDelta() - other.getDelta();
    }
}
