package io.gitlab.icestom.icestom.ui.theme;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.format.TextColor;

public interface Theme {
    Key getId();

    TextColor main();
    TextColor special();
    TextColor error();
    TextColor delta_positive();
    TextColor delta_equal();
    TextColor delta_negative();
}
