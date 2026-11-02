package io.gitlab.icestom.icestom.track;

import io.gitlab.icestom.icestom.track.library.VirtualTrackInstance;

import java.util.concurrent.CompletableFuture;

public interface TrackLoad {
    VirtualTrackInstance map();
    CompletableFuture<Track> metadataLoaded();
    CompletableFuture<Void> spawnLoaded();
    CompletableFuture<Void> fullyLoaded();

    void promote();
    void cancel();
}
