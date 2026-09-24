package io.gitlab.icestom.icestom.track.library;

import io.gitlab.icestom.icestom.config.IceStomConfig;
import io.gitlab.icestom.icestom.track.Track;
import io.gitlab.icestom.icestom.track.library.source.FileSystemSource;
import io.gitlab.icestom.icestom.track.library.source.TrackSource;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class TrackLibrary {

    private static final Logger log = LoggerFactory.getLogger(TrackLibrary.class);
    private static final Map<String, Function<URI, TrackSource>> registry = Map.of(
            "file", FileSystemSource::new
    );

    private final Map<String, TrackSource> sources = new HashMap<>();
    private final Map<String, String> loadPreferences = new HashMap<>();

    private final Map<String, UUID> nativeTrackIds = new HashMap<>();

    private final List<Ticket> tickets = new ArrayList<>();

    private final Map<UUID, Track> loadedTracks = new HashMap<>();
    private final Map<UUID, Integer> refCounts = new HashMap<>();

    public void init() {
        IceStomConfig.getConfig().library.forEach((id, uri_s) -> {
            URI uri = URI.create(uri_s);

            Function<URI, TrackSource> source_provider = registry.get(uri.getScheme());

            if (source_provider == null) {
                log.warn("Unknown library scheme: {}", uri);
                return;
            }

            TrackSource source = source_provider.apply(uri);

            sources.put(id, source);
        });

        loadTracks();
    }

    private void loadTracks() {
        sources.forEach((id, trackSource) -> {
            log.info("Preloading source '{}' [{}]", id, trackSource.getClass().getSimpleName());

            for (String track_id : trackSource.preloadTracks()) {
                loadPreferences.computeIfAbsent(track_id, _ -> id);
            }
        });
    }

    public @NotNull Optional<Ticket> loadTrack(String track_id) {
        UUID instance_id = nativeTrackIds.computeIfAbsent(track_id, _ -> UUID.randomUUID());

        Track track = loadedTracks.get(instance_id);

        if (track != null) {
            return Optional.of(new Ticket(instance_id, CompletableFuture.completedFuture(track)));
        }

        return loadTrackExclusive(track_id, instance_id);
    }

    public @NotNull Optional<Ticket> loadTrackExclusive(String track_id, UUID instance_id) {
        String source_id = loadPreferences.get(track_id);

        if (source_id == null) {
            log.warn("Attempt to load unknown track '{}'.", track_id);
            return Optional.empty();
        }

        TrackSource source = sources.get(source_id);

        if (source == null) {
            log.warn("Attempt to load '{}' from unknown source '{}'.", track_id, source_id);
            return Optional.empty();
        }

        return Optional.of(new Ticket(instance_id, source.loadTrack(track_id)));
    }

    public Set<String> getAvailableTracks() {
        return loadPreferences.keySet();
    }

    public @NotNull Map<String, TrackSource> getSources() {
        return Collections.unmodifiableMap(sources);
    }

    public @NotNull Map<UUID, Track> getLoadedTracks() {
        return Collections.unmodifiableMap(loadedTracks);
    }

    public @NotNull Map<UUID, Integer> getRefCounts() {
        return Collections.unmodifiableMap(refCounts);
    }

    public class Ticket {
        private final UUID trackInstanceId;
        private final CompletableFuture<Track> track;

        private State state = State.LOADING;

        private final long loadStart;

        private Ticket(UUID trackId, CompletableFuture<Track> track) {
            this.trackInstanceId = trackId;
            this.track = track;

            loadStart = System.nanoTime();

            tickets.add(this);
            refCounts.merge(trackId, 1, Integer::sum);

            if (track.isDone()) {
                state = State.LOADED;
                return;
            }

            track.whenComplete((fresh, throwable) -> {
                if (throwable != null) {
                    log.error("Track load failed", throwable);
                    return;
                }

                loadedTracks.put(trackId, fresh);

                synchronized (this) {
                    if (state == State.BURNT) {
                        return;
                    }
                    state = State.LOADED;
                }

                long loadEnd = System.nanoTime();
                log.info("Ticket[{}] load time: {}ns", trackId, loadEnd - loadStart);
            });
        }

        public void burn() {
            this.state = State.BURNT;

            if (!track.isDone()) track.completeExceptionally(new InterruptedException());

            if (tickets.remove(this)) {
                int refs = refCounts.getOrDefault(trackInstanceId, 0);

                if (refs <= 0) {
                    log.error("Ticket unaccounted for!");
                    return;
                }

                if (refs == 1) {
                    refCounts.remove(trackInstanceId);
                    loadedTracks.remove(trackInstanceId);
                } else {
                    refCounts.put(trackInstanceId, refs - 1);
                }
            }
        }

        public UUID getTrackInstanceId() {
            return trackInstanceId;
        }

        public State getState() {
            return state;
        }

        public CompletableFuture<Track> getTrack() {
            return track;
        }

        public enum State {
            LOADING,
            LOADED,
            BURNT
        }
    }
}
