package io.gitlab.icestom.icestom.ui;

import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;

public interface ActionBarProvider {
    Component getActionBar(Player player);
}
