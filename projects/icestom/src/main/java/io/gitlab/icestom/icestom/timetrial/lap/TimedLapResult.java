package io.gitlab.icestom.icestom.timetrial.lap;

import io.gitlab.icestom.icestom.timetrial.Split;
import net.minestom.server.coordinate.Pos;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record TimedLapResult(
        UUID uuid,
        List<Split> splits,
        Map<Integer, Pos> ticks
) implements TimedLapResultSource {
    public static TimedLapResult freeze(TimedLapResultSource timedLapResultSource) {
        return new TimedLapResult(
                 timedLapResultSource.uuid(),
                timedLapResultSource.splits(),
                timedLapResultSource.ticks()
        );
    }
}
