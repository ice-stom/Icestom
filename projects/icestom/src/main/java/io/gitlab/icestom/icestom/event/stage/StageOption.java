package io.gitlab.icestom.icestom.event.stage;

import org.jetbrains.annotations.Nullable;

import java.util.List;

public record StageOption(
        String name,
        Type type,
        String label,
        String description,
        boolean required,
        @Nullable Object defaultValue,
        @Nullable Double min,
        @Nullable Double max,
        @Nullable List<String> choices
) {
    public enum Type {
        STRING,
        NUMBER,
        BOOLEAN,
        TRACK
    }

    public static StageOption string(String name, String label, String description, @Nullable String defaultValue) {
        return new StageOption(name, Type.STRING, label, description, defaultValue == null, defaultValue, null, null, null);
    }

    public static StageOption track(String name, String label, String description) {
        return new StageOption(name, Type.TRACK, label, description, true, null, null, null, null);
    }

    public static StageOption number(String name, String label, String description, double defaultValue, @Nullable Double min, @Nullable Double max) {
        return new StageOption(name, Type.NUMBER, label, description, false, defaultValue, min, max, null);
    }

    public static StageOption bool(String name, String label, String description, boolean defaultValue) {
        return new StageOption(name, Type.BOOLEAN, label, description, false, defaultValue, null, null, null);
    }

    public static StageOption choice(String name, String label, String description, String defaultValue, String... choices) {
        return new StageOption(name, Type.STRING, label, description, false, defaultValue, null, null, List.of(choices));
    }
}
