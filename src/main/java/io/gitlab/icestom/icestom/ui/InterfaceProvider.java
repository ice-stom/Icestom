package io.gitlab.icestom.icestom.ui;

import net.minestom.server.entity.Player;

public interface InterfaceProvider {
    void startViewing(Player viewer);
    void stopViewing(Player viewer);
}
