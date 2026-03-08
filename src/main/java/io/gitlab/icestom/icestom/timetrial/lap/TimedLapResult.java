package io.gitlab.icestom.icestom.timetrial.lap;

import io.gitlab.icestom.icestom.timetrial.Split;

import java.util.List;

public class TimedLapResult implements TimedLapResultSource {

    private final List<Split> splits;

    public TimedLapResult(List<Split> splits) {
        this.splits = splits;
    }

    @Override
    public List<Split> getSplits() {
        return splits;
    }

    public static TimedLapResult freeze(TimedLapResultSource timedLapResultSource) {
        return new TimedLapResult(
                timedLapResultSource.getSplits()
        );
    }
}
