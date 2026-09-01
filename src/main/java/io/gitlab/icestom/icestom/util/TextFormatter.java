package io.gitlab.icestom.icestom.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentBuilder;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

import java.text.DecimalFormat;

public class TextFormatter {
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.000");

    private static final char[] ICESTOM_LOGO_CHAR = { '★', '◆', '●', '⬩', '·' };
    private static final int[] ICESTOM_LOGO_COLOR = { 0x9CC9FC, 0x84BBFB, 0x6BAEFA, 0x53A0F9, 0x3A93F8 };

    public static Component getTime(long ms) {
        long minutes = ms / (1000 * 60) % 60;
        double seconds = (ms % 60_000) / 1000.0;

        if (minutes > 0) {
            return Component.text(String.format("%d:%s", minutes, DECIMAL_FORMAT.format(seconds)));
        }
        return Component.text(DECIMAL_FORMAT.format(seconds));
    }

    public static Component getTimeRounded(long ms) {
        return getTime((long) (Math.ceil((double) ms / 50) * 50));
    }

    public static Component getDelta(long ms) {
        if (ms == 0) {
            return Component.text("=", NamedTextColor.YELLOW).append(getTime(0));
        }

        boolean positive = ms > 0;
        return Component.text(positive ? '+' : '-', positive ? NamedTextColor.RED : NamedTextColor.GREEN)
                .append(getTime(Math.abs(ms)));
    }

    public static Component getIcestomLogoRow(int y) {
        TextComponent.Builder component = Component.text();

        y -= 4;

        for (int x = -4; x <= 4; x++) {
            int d = Math.max(Math.abs(x), Math.abs(y));

            if ((x + y) % 2 == 0) {
                component.append(Component.text(ICESTOM_LOGO_CHAR[d], TextColor.color(ICESTOM_LOGO_COLOR[d])));
            } else {
                component.append(Component.space());
            }

            if (Math.abs(y) == 4 && (x == -2 || x == 2)) {
                component.append(Component.space());
            }

            component.append(Component.space());
        }

        return component.build();
    }
}