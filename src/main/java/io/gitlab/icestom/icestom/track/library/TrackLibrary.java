package io.gitlab.icestom.icestom.track.library;

import io.gitlab.icestom.icestom.config.IceStomConfig;
import io.gitlab.icestom.icestom.track.Track;
import io.gitlab.icestom.icestom.track.library.source.FileSystemSource;
import io.gitlab.icestom.icestom.track.library.source.TrackSource;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

public class TrackLibrary {

    private static final Logger log = LoggerFactory.getLogger(TrackLibrary.class);
    private static final Map<String, Function<URI, TrackSource>> registry = Map.of(
            "file", FileSystemSource::new
    );

    private final Map<String, TrackSource> sources = new HashMap<>();

    private final Map<String, String> load_preferences = new HashMap<>();

    private final Map<String, Track> loadedTracks = new HashMap<>();

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
                load_preferences.computeIfAbsent(track_id, _ -> id);
            }
        });
    }

    public @Nullable Track loadTrack(String track_id) {

        Track preloaded = loadedTracks.get(track_id);

        if (preloaded != null) {
            return preloaded;
        }

        String source_id = load_preferences.get(track_id);

        if (source_id == null) {
            log.warn("Attempt to load unknown track '{}'.", track_id);
            return null;
        }

        TrackSource source = sources.get(source_id);

        if (source == null) {
            log.warn("Attempt to load  '{}' from unknown source '{}'.", track_id, source_id);
            return null;
        }

        Optional<Track> track = source.getTrack(track_id);

        if (track.isEmpty()) {
            log.warn("Failed to load '{}' from '{}'.", track_id, source_id);
            return null;
        }

        return track.get();
    }

    public Set<String> getAvailableTracks() {
        return load_preferences.keySet();
    }
}
