package io.gitlab.icestom.icestom.event;

import io.gitlab.icestom.icestom.IceStom;
import io.gitlab.icestom.icestom.instance.PlayerHolder;
import net.kyori.adventure.key.Key;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface EventStage extends PlayerHolder {
    CompletableFuture<List<Result<EventParticipant>>> begin(List<Result<EventParticipant>> results);
    void cleanup();

    default void teleportAllParticipants(List<Result<EventParticipant>> results) {
        for (Result<EventParticipant> result : results) {
            for (Player participant : result.getParticipant().getParticipants()) {
                if (participant.getInstance() instanceof PlayerHolder holder) {
                    holder.drop(participant);
                    this.consume(participant);
                }
            }
        }
    }

    static @NotNull CompletableFuture<? extends EventStage> makeStage(Key type, Map<String, Object> options) {
        var constructor = IceStom.getInstance().getStageRegistry().getConstructor(type);
        if (constructor == null) {
            return CompletableFuture.failedFuture(new StageNotFoundException("Failed to find stage with type " + type));
        }
        return constructor.apply(options);
    }
}
