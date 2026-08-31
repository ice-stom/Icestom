package io.gitlab.icestom.icestom.event.event;

import io.gitlab.icestom.icestom.event.EventParticipant;
import net.minestom.server.entity.Player;
import net.minestom.server.event.trait.PlayerEvent;
import org.jetbrains.annotations.NotNull;

public interface EventParticipantEvent extends PlayerEvent {
    EventParticipant getParticipant();

    @Override
    default @NotNull Player getPlayer() {
        return getParticipant().getCurrentPlayer();
    };
}
