package io.gitlab.icestom.icestom.event;

import net.minestom.server.entity.Player;

import java.util.UUID;

public abstract class IceStomEvent<Participant extends EventParticipant> implements EventStage, AutoCloseable {
    private final UUID id = UUID.randomUUID();

    public IceStomEvent() {}

    @Override
    public void consume(Player player) {}

    @Override
    public void drop(Player player) {}

    public UUID getId() {
        return id;
    }
}

