package io.gitlab.icestom.icestom.timetrial.event;

import io.gitlab.icestom.icestom.timetrial.TimeTrialingInstance;
import io.gitlab.icestom.icestom.timetrial.lap.TimedLap;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class TimeTrialLapTimerEvent implements TimeTrialEvent, TimedLapEvent {
    @NotNull private final TimedLap lap;
    @NotNull private final Player player;
    @NotNull private final TimeTrialingInstance instance;

    public TimeTrialLapTimerEvent(@NotNull TimeTrialingInstance instance, @NotNull TimedLap lap, @NotNull Player player) {
        this.lap = lap;
        this.player = player;
        this.instance = instance;
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
