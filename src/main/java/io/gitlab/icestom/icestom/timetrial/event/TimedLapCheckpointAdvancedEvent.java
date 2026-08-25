package io.gitlab.icestom.icestom.timetrial.event;

import io.gitlab.icestom.icestom.timetrial.TimeTrialingInstance;
import io.gitlab.icestom.icestom.timetrial.lap.TimedLap;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class TimedLapCheckpointAdvancedEvent implements TimedLapEvent {
    @NotNull private final TimedLap lap;
    @NotNull private final Player player;

    public TimedLapCheckpointAdvancedEvent(@NotNull TimedLap lap, @NotNull Player player) {
        this.lap = lap;
        this.player = player;
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
