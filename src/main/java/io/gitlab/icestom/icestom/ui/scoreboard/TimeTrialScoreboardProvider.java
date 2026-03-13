package io.gitlab.icestom.icestom.ui.scoreboard;

import io.gitlab.icestom.icestom.timetrial.TimeTrialingInstance;

public interface TimeTrialScoreboardProvider extends ScoreboardProvider {
    void dispatchTimeTrialLeaderboard(TimeTrialingInstance instance);
}
