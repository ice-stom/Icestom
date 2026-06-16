package io.gitlab.icestom.icestom.timetrial.event;

import io.gitlab.icestom.icestom.timetrial.TimeTrialingInstance;
import io.gitlab.icestom.icestom.timetrial.lap.TimedLap;
import io.gitlab.icestom.icestom.timetrial.lap.TimedLapResult;
import io.gitlab.icestom.icestom.timetrial.lap.TimedLapResultSource;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TimeTrialTimedLapEndedEvent implements TimeTrialEvent, TimedLapEvent {
    @NotNull private final TimedLapResultSource result;
    @Nullable private final TimedLapResultSource previousBest;

    @NotNull private final TimedLap lap;
    @NotNull private final Player player;
    @NotNull private final TimeTrialingInstance instance;

    public TimeTrialTimedLapEndedEvent(@NotNull TimeTrialingInstance instance, @NotNull TimedLap lap, @NotNull Player player, @NotNull TimedLapResultSource result, @Nullable TimedLapResultSource previousBest) {
        this.result = result;
        this.previousBest = previousBest;
        this.lap = lap;
        this.player = player;
        this.instance = instance;
    }

    public TimeTrialTimedLapEndedEvent(@NotNull TimeTrialingInstance instance, @NotNull TimedLap lap, @NotNull Player player, @Nullable TimedLapResultSource previousBest) {
        this.result = TimedLapResult.freeze(lap);
        this.previousBest = previousBest;
        this.lap = lap;
        this.player = player;
        this.instance = instance;
    }

    public @NotNull TimedLapResultSource getResult() {
        return result;
    }

    public @Nullable TimedLapResultSource getPreviousBest() {
        return previousBest;
    }

    @Override
    public @NotNull TimedLap getLap() {
        return lap;
    }

    @Override
    public @NotNull Player getPlayer() {
        return player;
    }

    @Override
    public @NotNull TimeTrialingInstance getInstance() { return instance; }
}
