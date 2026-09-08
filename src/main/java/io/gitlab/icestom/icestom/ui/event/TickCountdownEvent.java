package io.gitlab.icestom.icestom.ui.event;

import io.gitlab.icestom.icestom.ui.TickCountdown;
import net.minestom.server.event.Event;

public interface TickCountdownEvent extends Event {
    TickCountdown getCountdown();
}
