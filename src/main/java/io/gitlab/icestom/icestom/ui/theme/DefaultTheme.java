package io.gitlab.icestom.icestom.ui.theme;

import io.gitlab.icestom.icestom.IceStom;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

public class DefaultTheme implements Theme {
    protected DefaultTheme() {}

    @Override
    public Key getId() {
        return Key.key(IceStom.NAMESPACE, "default");
    }

    @Override public TextColor main() { return NamedTextColor.WHITE; }
    @Override public TextColor special() { return NamedTextColor.YELLOW; }
    @Override public TextColor error() { return NamedTextColor.RED; }
    @Override public TextColor delta_positive() { return NamedTextColor.GREEN; }
    @Override public TextColor delta_equal() { return NamedTextColor.YELLOW; }
    @Override public TextColor delta_negative() { return NamedTextColor.RED; }
}
