package io.gitlab.icestom.icestom.track;

import io.gitlab.icestom.icestom.track.library.VirtualTrackInstance;
import net.minestom.server.instance.InstanceContainer;

import java.util.concurrent.CompletableFuture;

public interface TrackLoad {
    VirtualTrackInstance instanceContainer();
    CompletableFuture<Void> spawnLoaded();
    CompletableFuture<Void> fullyLoaded();
}
