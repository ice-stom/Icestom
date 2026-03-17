package io.gitlab.icestom.icestom.timetrial.event;

import io.gitlab.icestom.icestom.timetrial.lap.TimedLap;
import io.gitlab.icestom.icestom.timetrial.lap.TimedLapResultSource;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LapCompletedEvent implements TimeTrialEvent {
    @NotNull private final TimedLap lap;
    @NotNull private final Player player;
    @NotNull private final TimedLapResultSource result;
    @Nullable private final Long improvement;

    public LapCompletedEvent(@NotNull TimedLap lap, @NotNull Player player, @NotNull TimedLapResultSource result, @Nullable Long improvement) {
        this.lap = lap;
        this.player = player;
        this.result = result;
        this.improvement = improvement;
    }

    public LapCompletedEvent(@NotNull TimedLap lap, @NotNull Player player, @Nullable Long improvement) {
        this.lap = lap;
        this.player = player;
        this.result = lap;
        this.improvement = improvement;
    }

    @Override
    public @NotNull TimedLap getLap() {
        return lap;
    }

    @Override
    public @NotNull Player getPlayer() {
        return player;
    }

    public @NotNull TimedLapResultSource getResult() {
        return result;
    }

    public @Nullable Long getImprovement() {
        return improvement;
    }
}
