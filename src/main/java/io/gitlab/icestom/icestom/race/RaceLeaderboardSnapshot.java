package io.gitlab.icestom.icestom.race;

import io.gitlab.icestom.icestom.leaderboard.LeaderboardSnapshot;

public interface RaceLeaderboardSnapshot<R> extends LeaderboardSnapshot<R, RaceStage.RaceParticipant> {

    String getTrackName();

    int getTotalLaps();
    int getTotalPits();

    RaceStage.RaceParticipant getFlapHolder();

    default long getDelta(RaceLeaderboardRow self, RaceLeaderboardRow other) {
        return self.getDelta() - other.getDelta();
    }
}
