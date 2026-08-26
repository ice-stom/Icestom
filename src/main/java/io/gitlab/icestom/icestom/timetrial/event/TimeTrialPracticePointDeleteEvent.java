package io.gitlab.icestom.icestom.timetrial.event;

import io.gitlab.icestom.icestom.timetrial.TimeTrialingInstance;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

public class TimeTrialPracticePointDeleteEvent implements TimeTrialEvent {

    @NotNull private final Player player;
    @NotNull private final TimeTrialingInstance instance;

    public TimeTrialPracticePointDeleteEvent(@NotNull Player player, @NotNull TimeTrialingInstance instance) {
        this.player = player;
        this.instance = instance;
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
