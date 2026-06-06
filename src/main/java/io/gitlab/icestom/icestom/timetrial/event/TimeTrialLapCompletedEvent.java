package io.gitlab.icestom.icestom.timetrial.event;

import io.gitlab.icestom.icestom.timetrial.TimeTrialingInstance;
import io.gitlab.icestom.icestom.timetrial.lap.TimedLap;
import io.gitlab.icestom.icestom.timetrial.lap.TimedLapResult;
import io.gitlab.icestom.icestom.timetrial.lap.TimedLapResultSource;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TimeTrialLapCompletedEvent implements TimeTrialSessionEvent, TimedLapEvent {
    @NotNull private final TimedLapResultSource result;
    @Nullable private final Long deltaToPreviousBest;

    @NotNull private final TimedLap lap;
    @NotNull private final Player player;
    @NotNull private final TimeTrialingInstance instance;

    public TimeTrialLapCompletedEvent(@NotNull TimeTrialingInstance instance, @NotNull TimedLap lap, @NotNull Player player, @NotNull TimedLapResultSource result, @Nullable Long deltaToPreviousBest) {
        this.result = result;
        this.deltaToPreviousBest = deltaToPreviousBest;
        this.lap = lap;
        this.player = player;
        this.instance = instance;
    }

    public TimeTrialLapCompletedEvent(@NotNull TimeTrialingInstance instance, @NotNull TimedLap lap, @NotNull Player player, @Nullable Long deltaToPreviousBest) {
        this.result = TimedLapResult.freeze(lap);
        this.deltaToPreviousBest = deltaToPreviousBest;
        this.lap = lap;
        this.player = player;
        this.instance = instance;
    }

    public @NotNull TimedLapResultSource getResult() {
        return result;
    }
    public @Nullable Long getDeltaToPreviousBest() {
        return deltaToPreviousBest;
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
