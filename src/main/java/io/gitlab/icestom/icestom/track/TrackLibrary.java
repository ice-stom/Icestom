package io.gitlab.icestom.icestom.track;

import io.gitlab.icestom.icestom.track.format.TrackFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class TrackLibrary {

    public static final Path TRACK_STORAGE_PATH = Path.of("tracks");

    private static final Logger log = LoggerFactory.getLogger(TrackLibrary.class);
    private final Map<String, Track> tracks = new HashMap<>();

    public void init() {
        log.info("Track Storage: {}", TRACK_STORAGE_PATH.toAbsolutePath());

        loadTracks();
    }

    private void loadTracks() {
        boolean created = TRACK_STORAGE_PATH.toFile().mkdirs();

        if (created) return;

        File dir = TRACK_STORAGE_PATH.toFile();
        File[] files = dir.listFiles();

        if (files == null) {
            log.error("Failed to list files in tracks directory");
            return;
        }

        for (File file : files) {
            if (file.getName().endsWith("." + TrackFormat.FILE_EXTENSION)) {
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
