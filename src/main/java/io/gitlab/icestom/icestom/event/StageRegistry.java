package io.gitlab.icestom.icestom.event;

import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class StageRegistry {
    private final Map<Key, Function<Map<String, Object>, CompletableFuture<? extends EventStage>>> stages = new HashMap<>();

    private final Map<Class<? extends EventStage>, Key> reverse_lookup = new HashMap<>();

    private final Map<Key, StageSchema> schemas = new HashMap<>();

    public Function<Map<String, Object>, CompletableFuture<? extends EventStage>> getConstructor(Key key) {
        return stages.get(key);
    }

    public <T extends EventStage> void register(
            Key key,
            Class<T> tClass,
            Function<Map<String, Object>, CompletableFuture<T>> constructor
    ) {
        register(key, tClass, constructor, null);
    }

    public <T extends EventStage> void register(
            Key key,
            Class<T> tClass,
            Function<Map<String, Object>, CompletableFuture<T>> constructor,
            @Nullable StageSchema schema
    ) {
        reverse_lookup.put(tClass, key);

        stages.put(key, constructor::apply);

        if (schema != null) schemas.put(key, schema);
    }

    public @Nullable Key getKey(Class<?> tClass) {
        return reverse_lookup.get(tClass);
    }

    public @Nullable StageSchema getSchema(Key key) {
        return schemas.get(key);
    }

    public Set<Key> getKeys() {
        return Collections.unmodifiableSet(stages.keySet());
    }
}
