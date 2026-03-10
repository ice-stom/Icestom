package io.gitlab.icestom.icestom.ui.leaderboard;

import io.gitlab.icestom.icestom.race.Race;

public interface RaceLeaderboardProvider extends LeaderboardProvider {
    void dispatchRaceLeaderboard(Race race);
}
