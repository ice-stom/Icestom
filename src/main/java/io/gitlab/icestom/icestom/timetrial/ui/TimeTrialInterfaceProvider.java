package io.gitlab.icestom.icestom.timetrial.ui;

import io.gitlab.icestom.icestom.timetrial.TimeTrialingInstance;
import io.gitlab.icestom.icestom.ui.InterfaceProvider;

public interface TimeTrialInterfaceProvider extends InterfaceProvider {
    void updateTimeTrialLeaderboard(TimeTrialingInstance instance);
    void updateTimeTrialLapTimer(TimeTrialingInstance instance);
}
