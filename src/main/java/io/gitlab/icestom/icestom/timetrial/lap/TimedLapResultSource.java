package io.gitlab.icestom.icestom.timetrial.lap;

import io.gitlab.icestom.icestom.timetrial.Split;

import java.util.List;

public interface TimedLapResultSource {
    List<Split> getSplits();

    default long getSplitTime(int index) {
        return getSplits().get(index).ms();
    }

    default long getTime() {
        return getSplits().getLast().ms();
    }
}
