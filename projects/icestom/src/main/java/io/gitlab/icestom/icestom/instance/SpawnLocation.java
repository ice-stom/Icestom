package io.gitlab.icestom.icestom.instance;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.InstanceContainer;

public interface SpawnLocation extends PlayerHolder {
    Pos spawnLocation(Player player);

    default void resetPlayer(Player player) {
        player.teleport(spawnLocation(player));
    }

    @Override
    default void consume(Player player) {
        Pos spawn = spawnLocation(player);

        player.setRespawnPoint(spawn);

        if (player.getInstance() == this) {
            resetPlayer(player);
        } else {
            if (this instanceof Instance instance) {
                player.setInstance(instance, spawn)
                        .thenRun(() -> resetPlayer(player));
            }
        }
    }

    @Override
    default void drop(Player player) {}
}
