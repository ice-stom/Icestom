package io.gitlab.icestom.icestom.ui.event;

import net.kyori.adventure.text.Component;
import net.minestom.server.event.Event;

public interface MessageEvent extends Event {
    Component getComponent();
}
