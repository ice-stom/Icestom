package io.gitlab.icestom.icestom.instance;

import net.minestom.server.entity.Player;

public interface PlayerHolder {
    void consume(Player player);
    void drop(Player player);
}
