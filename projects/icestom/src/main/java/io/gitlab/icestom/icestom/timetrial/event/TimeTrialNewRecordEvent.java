package io.gitlab.icestom.icestom.timetrial.event;

import io.gitlab.icestom.icestom.timetrial.TimeTrialingInstance;
import io.gitlab.icestom.icestom.timetrial.lap.TimedLap;
import io.gitlab.icestom.icestom.timetrial.lap.TimedLapResultSource;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TimeTrialNewRecordEvent implements TimeTrialInstanceEvent, TimedLapResultSourceEvent {
    @NotNull private final TimedLap lap;
    @NotNull private final Player player;
    @NotNull private final TimeTrialingInstance instance;
    @NotNull private final TimedLapResultSource result;
    @Nullable private final TimedLapResultSource oldResult;

    public TimeTrialNewRecordEvent(@NotNull TimedLap lap, @NotNull Player player, @NotNull TimeTrialingInstance instance, @NotNull TimedLapResultSource result, @Nullable TimedLapResultSource oldResult) {
        this.lap = lap;
        this.player = player;
        this.instance = instance;
        this.result = result;
        this.oldResult = oldResult;
    }

    @Override
    public @NotNull TimeTrialingInstance getInstance() {
        return instance;
    }

    @Override
    public @NotNull TimedLapResultSource getResult() {
        return result;
    }

    public @Nullable TimedLapResultSource getPreviousBest() {
        return oldResult;
    }

    @Override
    public @NotNull TimedLap getLap() {
        return lap;
    }

    @Override
    public @NotNull Player getPlayer() {
        return player;
    }
}