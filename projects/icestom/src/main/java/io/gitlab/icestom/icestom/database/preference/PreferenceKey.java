package io.gitlab.icestom.icestom.database.preference;

import net.kyori.adventure.key.Key;

public record PreferenceKey<T>(
        Key key,
        Class<T> type,
        T defaultValue
) {}