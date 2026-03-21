package io.gitlab.icestom.icestom.leaderboard;

import io.gitlab.icestom.icestom.timetrial.Split;

import java.util.List;

public interface LeaderboardParticipant<P extends LeaderboardParticipant<P>> {
    int getGlobalCheckpointIndex();
    List<Split> getSplits();

    long deltaTo(P other);
}
