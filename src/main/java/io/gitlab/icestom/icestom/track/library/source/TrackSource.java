package io.gitlab.icestom.icestom.track.library.source;

import io.gitlab.icestom.icestom.track.Track;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public abstract class TrackSource {
    private final URI uri;

    public TrackSource(URI uri) {
        this.uri = uri;
    };

    public URI getUri() {
        return uri;
    }

    public abstract List<String> preloadTracks();
    public abstract @NotNull CompletableFuture<Optional<Track>> getTrack(String track_id);
}
