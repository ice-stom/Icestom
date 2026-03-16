package io.gitlab.icestom.icestom.ui;

import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.Nullable;

public interface ActionBarProvider {
    @Nullable Component getActionBar(Player player);
}
