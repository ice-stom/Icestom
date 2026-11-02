package io.gitlab.icestom.icestom.event.event;

import net.minestom.server.entity.Player;

import java.util.List;
import java.util.UUID;

public interface EventParticipant {
    UUID getUuid();

    Player getCurrentPlayer();
    List<UUID> getPlayers();
}
