package io.gitlab.icestom.icestom.event;

import io.gitlab.icestom.icestom.event.stage.Stage;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventHandler;
import net.minestom.server.event.EventListener;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class EventManager implements EventHandler<Event> {

    private final EventNode<Event> eventNode = EventNode.all("events");

    private final Map<String, ActiveEvent> events = new HashMap<>();

    public EventManager() {
        eventNode.addListener(AsyncPlayerConfigurationEvent.class, event -> {
            final Player player = event.getPlayer();

            @Nullable ActiveEvent active_event = getEvent(player);

            if (active_event != null) {
                @Nullable Stage<?> current_stage = active_event.getCurrentStage();

                if (current_stage != null) {
                    event.setSpawningInstance(current_stage.getInstance(player));

                    EventListener<@NotNull PlayerSpawnEvent> listener = EventListener.builder(PlayerSpawnEvent.class)
                            .filter(e -> e.getPlayer() == player)
                            .handler(_ -> {
                                current_stage.getInstance(player).consume(player);
                            })
                            .expireCount(1)
                            .build();

                    eventNode.addListener(listener);

                    return;
                }
            }
        });
    }

    public @Nullable ActiveEvent getEvent(Player player) {
        for (ActiveEvent event : events.values()) {
            if (event.hasParticipant(player.getUuid())) {
                return event;
            }
        }

        return null;
    }

    public void addEvent(ActiveEvent event) {
        events.put(event.getId(), event);
    }

    public void startEvent(ActiveEvent event) {
        if (event.getCurrentStage() != null) return;

        event.nextStage();
    }

    public @Nullable ActiveEvent getEvent(String id) {
        return events.get(id);
    }

    public Collection<ActiveEvent> getEvents() {
        return events.values();
    }

    @Override
    public @NotNull EventNode<Event> eventNode() {
        return eventNode;
    }
}
