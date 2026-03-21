package io.gitlab.icestom.icestom.race.ui;

import io.gitlab.icestom.icestom.race.Race;
import io.gitlab.icestom.icestom.ui.InterfaceProvider;

public interface RaceInterfaceProvider extends InterfaceProvider {
    void dispatchRaceLeaderboard(Race race);
}
