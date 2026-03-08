package io.gitlab.icestom.icestom.instance;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.InstanceContainer;

public interface SpawnLocation {
    Pos spawnLocation(Player player);

    default void resetPlayer(Player player) {
        player.teleport(spawnLocation(player));
    }

    default void consume(Player player) {
        if (player.getInstance() == this) {
            resetPlayer(player);
        } else {
            if (this instanceof InstanceContainer instanceContainer) {
                player.setInstance(instanceContainer, spawnLocation(player))
                        .thenRun(() -> resetPlayer(player));
            }
        }
    }

    default void drop(Player player) {}
}
