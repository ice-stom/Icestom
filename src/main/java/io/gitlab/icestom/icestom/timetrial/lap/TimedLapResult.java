package io.gitlab.icestom.icestom.timetrial.lap;

import io.gitlab.icestom.icestom.timetrial.Split;

import java.util.List;

public record TimedLapResult(List<Split> splits) implements TimedLapResultSource {

    public static TimedLapResult freeze(TimedLapResultSource timedLapResultSource) {
        return new TimedLapResult(
                timedLapResultSource.splits()
        );
    }
}
