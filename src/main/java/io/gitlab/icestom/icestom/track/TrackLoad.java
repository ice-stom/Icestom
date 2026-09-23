package io.gitlab.icestom.icestom.track;

import net.minestom.server.instance.InstanceContainer;

import java.util.concurrent.CompletableFuture;

public interface TrackLoad {
    InstanceContainer instanceContainer();
    CompletableFuture<Void> spawnLoaded();
    CompletableFuture<Void> fullyLoaded();
}
