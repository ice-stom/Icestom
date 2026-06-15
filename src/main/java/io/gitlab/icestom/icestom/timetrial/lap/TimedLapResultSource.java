package io.gitlab.icestom.icestom.timetrial.lap;

import io.gitlab.icestom.icestom.timetrial.Split;
import net.minestom.server.coordinate.Pos;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface TimedLapResultSource {
    List<Split> splits();

    Map<Integer, Pos> ticks();

    default long getSplitTime(int index) {
        return splits().get(index).ms();
    }

    default long getTime() {
        return splits().getLast().ms();
    }
}
