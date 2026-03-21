package io.gitlab.icestom.icestom.timetrial.event;

import io.gitlab.icestom.icestom.timetrial.TimeTrialingInstance;
import io.gitlab.icestom.icestom.timetrial.lap.TimedLap;
import io.gitlab.icestom.icestom.timetrial.lap.TimedLapResult;
import io.gitlab.icestom.icestom.timetrial.lap.TimedLapResultSource;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

public class TimeTrialLapCompletedEvent implements TimeTrialSessionEvent, TimedLapEvent {
    @NotNull private final TimedLapResultSource result;
    @Nullable private final Long improvement;

    @NotNull private final TimedLap lap;
    @NotNull private final Player player;
    @NotNull private final TimeTrialingInstance instance;

    public TimeTrialLapCompletedEvent(@NonNull TimeTrialingInstance instance, @NotNull TimedLap lap, @NotNull Player player, @NotNull TimedLapResultSource result, @Nullable Long improvement) {
        this.result = result;
        this.improvement = improvement;
        this.lap = lap;
        this.player = player;
        this.instance = instance;
    }

    public TimeTrialLapCompletedEvent(@NonNull TimeTrialingInstance instance, @NotNull TimedLap lap, @NotNull Player player, @Nullable Long improvement) {
        this.result = TimedLapResult.freeze(lap);
        this.improvement = improvement;
        this.lap = lap;
        this.player = player;
        this.instance = instance;
    }

    public @NotNull TimedLapResultSource getResult() {
        return result;
    }
    public @Nullable Long getImprovement() {
        return improvement;
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
    public @NonNull TimeTrialingInstance getInstance() { return instance; }
}
