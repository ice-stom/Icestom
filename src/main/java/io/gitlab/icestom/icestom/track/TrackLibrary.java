package io.gitlab.icestom.icestom.track;

import io.gitlab.icestom.icestom.track.format.TrackFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class TrackLibrary {

    public static final Path TRACK_STORAGE_PATH = Path.of("tracks");

    private static final Logger log = LoggerFactory.getLogger(TrackLibrary.class);
    private final Map<String, Track> tracks = new HashMap<>();

    public void init() {
        log.info("Track Storage: {}", TRACK_STORAGE_PATH.toAbsolutePath());

        loadTracks();
    }

    private void loadTracks() {
        boolean is_empty = TRACK_STORAGE_PATH.toFile().mkdirs();

        if (is_empty) return;

        for (File file : Objects.requireNonNull(TRACK_STORAGE_PATH.toFile().listFiles())) {
            if (file.getName().endsWith("." + TrackFormat.FILE_EXTENTION)) {
                log.info("Loading {}", file.getAbsolutePath());

                try {
                    Track track = TrackFormat.loadTrack(file);

                    tracks.put(track.getId(), track);
                } catch (IOException | TrackFormat.TrackLoadException e) {
                    log.error(e.toString());
                }
            }
        }
    }

    public Map<String, Track> getTracks() { return tracks; }
}
