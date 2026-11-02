package io.gitlab.icestom.icestom.event.event;

import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;

import java.util.List;
import java.util.UUID;

public interface EventParticipant {
    UUID getUuid();

    Component getName();

    Player getCurrentPlayer();
    List<UUID> getPlayers();
}
