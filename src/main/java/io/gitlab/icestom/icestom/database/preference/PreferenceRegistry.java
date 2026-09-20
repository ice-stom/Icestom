package io.gitlab.icestom.icestom.database.preference;

import net.kyori.adventure.key.Key;

import java.util.HashMap;
import java.util.Map;

public class PreferenceRegistry {
    private final Map<Key, PreferenceKey<?>> registry = new HashMap<>();

    public <T> PreferenceKey<T> register(PreferenceKey<T> key) {
        if (registry.putIfAbsent(key.key(), key) != null) {
            throw new IllegalArgumentException(
                    "Preference already registered: " + key.key()
            );
        }

        return key;
    }

    public PreferenceKey<?> get(Key name) {
        PreferenceKey<?> key = registry.get(name);

        if (key == null) {
            throw new IllegalArgumentException(
                    "Unknown preference: " + name
            );
        }

        return key;
    }
}
