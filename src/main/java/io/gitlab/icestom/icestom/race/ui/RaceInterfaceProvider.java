package io.gitlab.icestom.icestom.race.ui;

import io.gitlab.icestom.icestom.race.RaceInstance;
import io.gitlab.icestom.icestom.ui.InterfaceProvider;

public interface RaceInterfaceProvider extends InterfaceProvider {
    void updateRaceLeaderboard(RaceInstance instance);
    void updateRaceLapTimer(RaceInstance instance);
}
