package io.gitlab.icestom.icestom.event.stage;

import io.gitlab.icestom.icestom.IceStom;
import io.gitlab.icestom.icestom.event.event.EventParticipant;
import io.gitlab.icestom.icestom.event.event.Result;
import io.gitlab.icestom.icestom.instance.PlayerHolder;
import net.kyori.adventure.key.Key;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.network.ConnectionManager;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface EventStage extends PlayerHolder {

    String getStageName();

    CompletableFuture<List<Result<EventParticipant>>> begin(List<Result<EventParticipant>> results);

    void cleanup();

    default void teleportAllParticipants(List<Result<EventParticipant>> results) {
        ConnectionManager connectionManager = MinecraftServer.getConnectionManager();

        for (Result<EventParticipant> result : results) {
            for (UUID participant : result.getParticipant().getPlayers()) {
                Player player = connectionManager.getOnlinePlayerByUuid(participant);

                if (player == null) continue;

                if (player.getInstance() instanceof PlayerHolder holder) {
                    holder.drop(player);
                    this.consume(player);
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
