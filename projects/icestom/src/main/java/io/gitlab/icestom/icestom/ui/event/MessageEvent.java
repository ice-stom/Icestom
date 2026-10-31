package io.gitlab.icestom.icestom.ui.event;

import net.kyori.adventure.text.Component;
import net.minestom.server.event.Event;
import net.minestom.server.event.trait.PlayerEvent;

public interface MessageEvent extends Event, PlayerEvent {
    Component getComponent();
}
