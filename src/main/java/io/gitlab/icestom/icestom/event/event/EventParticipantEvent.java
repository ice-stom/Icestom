package io.gitlab.icestom.icestom.event.event;

import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.trait.PlayerEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public interface EventParticipantEvent extends PlayerEvent {
    EventParticipant getParticipant();

    @Override
    default @NotNull Player getPlayer() {
        return getParticipant().getCurrentPlayer();
    }
}
