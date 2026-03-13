package io.gitlab.icestom.icestom.race.scoreboard;

import io.gitlab.icestom.icestom.race.Race;
import io.gitlab.icestom.icestom.ui.scoreboard.ScoreboardProvider;

public interface RaceScoreboardProvider extends ScoreboardProvider {
    void dispatchRaceLeaderboard(Race race);
}
