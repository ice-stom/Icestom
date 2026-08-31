package io.gitlab.icestom.icestom.ui.event;

import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.event.trait.PlayerEvent;
import org.jetbrains.annotations.NotNull;

public class GenericErrorMessageEvent implements PlayerEvent, MessageEvent {
    private final @NotNull Player player;
    private final @NotNull Component component;

    public GenericErrorMessageEvent(@NotNull Player player, @NotNull Component component) {
        this.player = player;
        this.component = component;
    }

    @Override
    public @NotNull Player getPlayer() {
        return player;
    }

    public @NotNull Component getComponent() {
        return component;
    }
}
