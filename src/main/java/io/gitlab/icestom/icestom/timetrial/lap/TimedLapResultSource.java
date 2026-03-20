package io.gitlab.icestom.icestom.timetrial.lap;

import io.gitlab.icestom.icestom.timetrial.Split;

import java.util.List;

public interface TimedLapResultSource {
    List<Split> splits();

    default long getSplitTime(int index) {
        return splits().get(index).ms();
    }

    default long getTime() {
        return splits().getLast().ms();
    }
}
