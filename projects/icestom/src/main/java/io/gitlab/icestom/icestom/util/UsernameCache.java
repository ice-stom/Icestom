package io.gitlab.icestom.icestom.util;

import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.utils.mojang.MojangUtils;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class UsernameCache {

    private static final Map<UUID, String> name = new ConcurrentHashMap<>();
    private static final Set<UUID> negativeLookups = ConcurrentHashMap.newKeySet();

    private static final Map<UUID, CompletableFuture<@Nullable String>> jobs = new ConcurrentHashMap<>();

    public static CompletableFuture<@Nullable String> getUsername(UUID uuid) {
        if (negativeLookups.contains(uuid)) return CompletableFuture.completedFuture(null);

        final String cachedUsername = getUsernameCached(uuid);

        if (cachedUsername != null) return CompletableFuture.completedFuture(cachedUsername);

        return jobs.computeIfAbsent(uuid, uuid2 -> CompletableFuture.supplyAsync(() -> {
            try {
                @Nullable String name = MojangUtils.getUsername(uuid2);

                // trust me its nullable
                if (name == null) {
                    negativeLookups.add(uuid2);
                } else {
                    UsernameCache.name.put(uuid2, name);
                }

                return name;
            } catch (IOException e) {
                return null;
            } finally {
                jobs.remove(uuid2); // failed lookups retry
            }
        }));
    }

    public static @Nullable String getUsernameCached(UUID uuid) {
        return name.computeIfAbsent(uuid, uuid2 -> {
            Player player = MinecraftServer.getConnectionManager().getOnlinePlayerByUuid(uuid2);

            if (player != null) return player.getUsername();

            return null;
        });
    }
}
