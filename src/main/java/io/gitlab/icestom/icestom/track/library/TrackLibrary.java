package io.gitlab.icestom.icestom.track.library;

import io.gitlab.icestom.icestom.config.IceStomConfig;
import io.gitlab.icestom.icestom.track.Track;
import io.gitlab.icestom.icestom.track.TrackLoad;
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
    private final Map<UUID, String> nativeTrackIdsLookup = new HashMap<>();

    private final List<Ticket> tickets = new ArrayList<>();

    private final Map<UUID, TrackLoad> trackLoads = new HashMap<>();
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

    public @NotNull CompletableFuture<Optional<Ticket>> loadTrack(String track_id) {
        UUID instance_id = nativeTrackIds.computeIfAbsent(track_id, _ -> UUID.randomUUID());
        nativeTrackIdsLookup.put(instance_id, track_id);

        TrackLoad trackLoad = trackLoads.get(instance_id);

        if (trackLoad != null) {
            return CompletableFuture.completedFuture(Optional.of(new Ticket(instance_id, trackLoad)));
        }

        return loadTrackExclusive(track_id, instance_id);
    }

    public @NotNull CompletableFuture<Optional<Ticket>> loadTrackExclusive(String track_id, UUID instance_id) {
        String source_id = loadPreferences.get(track_id);

        if (source_id == null) {
            log.warn("Attempt to load unknown track '{}'.", track_id);
            return CompletableFuture.completedFuture(Optional.empty());
        }

        TrackSource source = sources.get(source_id);

        if (source == null) {
            log.warn("Attempt to load '{}' from unknown source '{}'.", track_id, source_id);
            return CompletableFuture.completedFuture(Optional.empty());
        }

        return source.loadTrack(track_id).thenApply(load -> {
            synchronized (TrackLibrary.this) {
                trackLoads.put(instance_id, load);
                return Optional.of(new Ticket(instance_id, load));
            }
        });
    }

    public Ticket newReference(Ticket ticket) {
        return new Ticket(ticket.getTrackInstanceId(), ticket.trackLoad);
    }

    public Set<String> getAvailableTracks() {
        return loadPreferences.keySet();
    }

    public @NotNull Map<String, TrackSource> getSources() {
        return Collections.unmodifiableMap(sources);
    }

    public @NotNull Map<UUID, TrackLoad> getTrackLoads() {
        return Collections.unmodifiableMap(trackLoads);
    }

    public @NotNull Map<UUID, Integer> getRefCounts() {
        return Collections.unmodifiableMap(refCounts);
    }

    public class Ticket {
        private final UUID trackInstanceId;
        private final TrackLoad trackLoad;

        private State state = State.LOADING;

        private final long loadStart;

        Ticket(UUID trackId, TrackLoad trackLoad) {
            this.trackInstanceId = trackId;
            this.trackLoad = trackLoad;

            log.info("load start");
            loadStart = System.nanoTime();

            tickets.add(this);
            refCounts.merge(trackId, 1, Integer::sum);

            if (trackLoad.fullyLoaded().isDone() && !trackLoad.fullyLoaded().isCompletedExceptionally()) {
                state = State.LOADED;
                return;
            }

            trackLoad.fullyLoaded().whenComplete((_, throwable) -> {
                if (throwable != null) {
                    log.error("Track load failed", throwable);
                    return;
                }

                synchronized (this) {
                    if (state == State.BURNT) {
                        return;
                    }
                    state = State.LOADED;
                }

                long loadEnd = System.nanoTime();
                log.info("Ticket[{}/{}] meta load time: {}ms", trackId, nativeTrackIdsLookup.getOrDefault(trackId, "??"), (loadEnd - loadStart) * 1e-6);
            });
        }

        public void promote() {
            trackLoad.promote();
        }

        public void burn() {
            synchronized (TrackLibrary.this) {
                if (state == State.BURNT) return;
                state = State.BURNT;
                if (!tickets.remove(this)) return;

                int refs = refCounts.getOrDefault(trackInstanceId, 0);
                if (refs <= 1) {
                    refCounts.remove(trackInstanceId);
                    trackLoads.remove(trackInstanceId, trackLoad);
                    trackLoad.cancel();
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

        public CompletableFuture<Track> getTrackMeta() {
            return trackLoad.metadataLoaded();
        }

        public CompletableFuture<Void> readyForTeleport() {
            return trackLoad.spawnLoaded();
        }

        public enum State {
            LOADING,
            LOADED,
            BURNT
        }
    }
}
