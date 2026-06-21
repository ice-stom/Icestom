package io.gitlab.icestom.icestom.plugins;

import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;

public interface IceStomPlugin {
    void onEnable(EventNode<Event> eventNode);
}
