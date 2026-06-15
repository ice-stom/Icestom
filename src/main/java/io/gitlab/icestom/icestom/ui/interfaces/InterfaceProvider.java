package io.gitlab.icestom.icestom.ui.interfaces;

import net.minestom.server.entity.Player;

public interface InterfaceProvider {
    <H, I extends Interface<H, I>> I getInterface(H holder);
    boolean supportsPlayer(Player player);
}
