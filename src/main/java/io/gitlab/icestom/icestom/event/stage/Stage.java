package io.gitlab.icestom.icestom.event.stage;

import io.gitlab.icestom.icestom.instance.PlayerHolder;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.InstanceContainer;

import java.util.Set;

public interface Stage<I extends InstanceContainer & PlayerHolder> extends PlayerHolder {
    I getInstance(Player player);

    Set<Player> getPlayers();

    @Override
    default void drop(Player player) {
        getInstance(player).drop(player);
    };

    void register();
    void unregister();
}
