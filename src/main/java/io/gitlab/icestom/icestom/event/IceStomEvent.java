package io.gitlab.icestom.icestom.event;

import io.gitlab.icestom.icestom.IceStom;
import io.gitlab.icestom.icestom.instance.SpawnInstance;
import net.minestom.server.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public abstract class IceStomEvent<Participant extends EventParticipant> implements EventStage {
    private final UUID id = UUID.randomUUID();

    private final List<EventStage> stages = new ArrayList<>();

    protected CompletableFuture<List<Result<EventParticipant>>> future;

    public IceStomEvent() {}

    protected void addStage(EventStage stage) {
        stages.add(stage);
    }

    @Override
    public Set<Player> getPlayers() {
        return stages.stream()
                .flatMap(stage -> stage.getPlayers().stream())
                .collect(Collectors.toSet());
    }

    @Override
    public void consume(Player player) {}

    @Override
    public void drop(Player player) {}

    public UUID getId() {
        return id;
    }

    @Override
    public void cleanup() {
        future.completeExceptionally(new EventCancelledException());

        stages.forEach(stage -> {
            SpawnInstance spawnInstance = IceStom.getInstance().getSpawnInstance();

            stage.getPlayers().forEach(player -> {
                stage.drop(player);
                spawnInstance.consume(player);
            });

            stage.cleanup();
        });
    }

    public static class EventCancelledException extends RuntimeException {
        public EventCancelledException() {
            super("Event cancelled");
        }
    }
}

