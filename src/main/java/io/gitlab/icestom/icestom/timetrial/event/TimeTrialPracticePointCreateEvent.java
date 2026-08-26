package io.gitlab.icestom.icestom.timetrial.event;

import io.gitlab.icestom.icestom.timetrial.TimeTrialingInstance;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

public class TimeTrialPracticePointCreateEvent implements TimeTrialEvent {

    @NotNull private final TimeTrialingInstance instance;
    @NotNull private final Player player;

    public TimeTrialPracticePointCreateEvent(@NotNull Player player, @NotNull TimeTrialingInstance instance) {
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
