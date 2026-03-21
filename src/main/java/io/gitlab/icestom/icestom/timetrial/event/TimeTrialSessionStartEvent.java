package io.gitlab.icestom.icestom.timetrial.event;

import io.gitlab.icestom.icestom.timetrial.TimeTrialingInstance;
import net.minestom.server.entity.Player;
import net.minestom.server.event.trait.PlayerEvent;
import org.jetbrains.annotations.NotNull;

public class TimeTrialSessionStartEvent implements TimeTrialSessionEvent, PlayerEvent {
    @NotNull private final TimeTrialingInstance instance;
    @NotNull private final Player player;

    public TimeTrialSessionStartEvent(@NotNull TimeTrialingInstance instance, @NotNull Player player) {
        this.instance = instance;
        this.player = player;
    }

    @Override
    public @NotNull TimeTrialingInstance getInstance() {
        return instance;
    }

    @Override
    public @NotNull Player getPlayer() {
        return player;
    }
}
