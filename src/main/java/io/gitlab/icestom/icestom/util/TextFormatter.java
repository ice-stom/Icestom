package io.gitlab.icestom.icestom.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.text.DecimalFormat;

public class TextFormatter {
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.000");

    public static Component getTime(long ms) {
        long minutes = ms / (1000 * 60) % 60;
        double seconds = (ms % 60_000) / 1000.0;

        if (minutes > 0) {
            return Component.text(String.format("%d:%s", minutes, DECIMAL_FORMAT.format(seconds)));
        }
        return Component.text(DECIMAL_FORMAT.format(seconds));
    }

    public static Component getDelta(long ms) {
        if (ms == 0) {
            return Component.text("=", NamedTextColor.YELLOW).append(getTime(0));
        }

        boolean positive = ms > 0;
        return Component.text(positive ? '+' : '-', positive ? NamedTextColor.RED : NamedTextColor.GREEN)
                .append(getTime(Math.abs(ms)));
    }
}