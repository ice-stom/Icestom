package io.gitlab.icestom.icestom.ui.interfaces;

import net.minestom.server.entity.Player;

import java.util.Set;

public abstract class Interface<H, I extends Interface<H, I>> {
    public abstract H getHolder();
    public abstract Set<Player> getWatching();

    public abstract void startWatching(Player player);
    public abstract void stopWatching(Player player);
}
