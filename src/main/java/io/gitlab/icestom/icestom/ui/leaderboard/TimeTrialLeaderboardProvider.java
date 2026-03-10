package io.gitlab.icestom.icestom.ui.leaderboard;

import io.gitlab.icestom.icestom.instance.TimeTrialingInstance;

public interface TimeTrialLeaderboardProvider extends LeaderboardProvider {
    void dispatchTimeTrialLeaderboard(TimeTrialingInstance instance);
}
