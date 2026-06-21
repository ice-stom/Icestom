package io.gitlab.icestom.icestom.util;

import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.utils.mojang.MojangUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class UsernameCache {

    private static final Map<UUID, String> name = new HashMap<>();
    private static final Set<UUID> negativeLookups = new HashSet<>();

    private static final Map<UUID, CompletableFuture<@Nullable String>> jobs = new HashMap<>();

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
                }

                return null;
            } catch (IOException e) {
                return null;
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
