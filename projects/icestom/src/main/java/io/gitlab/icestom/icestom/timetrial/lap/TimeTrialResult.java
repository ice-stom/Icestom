package io.gitlab.icestom.icestom.timetrial.lap;

import io.gitlab.icestom.icestom.timetrial.Split;
import net.minestom.server.coordinate.Pos;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record TimeTrialResult(
        @NotNull UUID uuid,
        @NotNull UUID player,
        @NotNull String track,
        @NotNull List<Split> splits,
        @NotNull Map<Integer, Pos> ticks
) implements TimedLapResultSource {
    public static TimeTrialResult fromResult(UUID player, String track, TimedLapResultSource timedLapResultSource) {
        return new TimeTrialResult(
                timedLapResultSource.uuid(),
                player,
                track,
                timedLapResultSource.splits(),
                timedLapResultSource.ticks()
        );
    }
}
