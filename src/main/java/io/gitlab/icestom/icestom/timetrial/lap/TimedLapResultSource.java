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

    default boolean isBetterThan(TimedLapResultSource other) {

        boolean is_first = other == null;
        boolean is_best_checkpoints = !is_first && this.splits().size() > other.splits().size();
        boolean is_best_time = !is_first && this.splits().size() == other.splits().size() && this.getTime() < other.getTime();

        if (is_first || is_best_checkpoints || is_best_time) {
            return true;
        } else {
            return false;
        }
    }
}
