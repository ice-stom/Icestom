package io.gitlab.icestom.icestom.instance;

import net.minestom.server.entity.Player;

import java.util.Set;

public interface PlayerHolder {
    void consume(Player player);
    void drop(Player player);

    Set<Player> getPlayers();
}
