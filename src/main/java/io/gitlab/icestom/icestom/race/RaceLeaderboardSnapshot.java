package io.gitlab.icestom.icestom.race;

import io.gitlab.icestom.icestom.leaderboard.LeaderboardSnapshot;

public interface RaceLeaderboardSnapshot<R> extends LeaderboardSnapshot<R, RaceInstance.RaceParticipant> {

    String getTrackName();

    int getTotalLaps();
    int getTotalPits();

    RaceInstance.RaceParticipant getFlapHolder();

    default long getDelta(RaceLeaderboardRow self, RaceLeaderboardRow other) {
        return self.getDelta() - other.getDelta();
    }
}
