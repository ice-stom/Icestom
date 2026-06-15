package io.gitlab.icestom.icestom.timetrial.lap;

import io.gitlab.icestom.icestom.timetrial.Split;
import net.minestom.server.coordinate.Pos;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record TimeTrialResult(
        UUID player,
        String track,
        List<Split> splits,
        Map<Integer, Pos> ticks
) implements TimedLapResultSource {
    public static TimeTrialResult fromResult(UUID player, String track, TimedLapResultSource timedLapResultSource) {
        return new TimeTrialResult(
                player,
                track,
                timedLapResultSource.splits(),
                timedLapResultSource.ticks()
        );
    }
}
