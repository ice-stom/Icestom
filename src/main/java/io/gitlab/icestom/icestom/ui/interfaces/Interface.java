package io.gitlab.icestom.icestom.ui.interfaces;

import net.minestom.server.entity.Player;

import java.util.HashSet;
import java.util.Set;

public abstract class Interface<H, I extends Interface<H, I>> {

    private final Set<Player> watching = new HashSet<>();

    public abstract H getHolder();

    public Set<Player> getWatching() {
        return watching;
    };

    public void startWatching(Player player) {
        watching.add(player);
    }

    public void stopWatching(Player player) {
        watching.remove(player);
    }
}
