package io.gitlab.icestom.icestom.event;

import net.minestom.server.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class EventManager {
    private final Map<String, Event> events = new HashMap<>();

    public @Nullable Event getEvent(Player player) {
        for (Event event : events.values()) {
            if (event.hasParticipant(player.getUuid())) {
                return event;
            }
        }

        return null;
    }

    public void addEvent(Event event) {
        events.put(event.getId(), event);
    }

    public void startEvent(Event event) {
        if (event.getCurrentStage() != null) return;

        event.nextStage();
    }

    public @Nullable Event getEvent(String id) {
        return events.get(id);
    }

    public Collection<Event> getEvents() {
        return events.values();
    }
}
