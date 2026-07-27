package io.gitlab.icestom.icestom.track.library.source;

import io.gitlab.icestom.icestom.instance.TrackInstance;
import io.gitlab.icestom.icestom.track.Track;
import io.gitlab.icestom.stomtrack.EnvironmentFile;
import io.gitlab.icestom.stomtrack.TrackFile;
import io.gitlab.icestom.stomtrack.TrackLoader;
import net.hollowcube.polar.PolarLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class FileSystemSource extends TrackSource {

    private static final Logger log = LoggerFactory.getLogger(FileSystemSource.class);
    private final Path folder;

    private final Map<String, Path> sourceFiles = new LinkedHashMap<>();
    private final Map<String, Track> tracks = new HashMap<>();

    public FileSystemSource(URI uri) {
        super(uri);

        if (uri.getAuthority().isEmpty()) {
            folder = Path.of(uri);
        } else {
            if (uri.getPath().charAt(0) != '/') throw new RuntimeException("Bad filesystem path.");

            folder = Path.of("").resolve(uri.getPath().substring(1));
        }

        if (!folder.toFile().exists()) {
            boolean _ = folder.toFile().mkdirs();
        }
    }

    @Override
    public List<String> preloadTracks() {
        File dir = folder.toFile();
        File[] files = dir.listFiles((file, s) -> s.endsWith(".stomtrack"));

        if (files == null) throw new IllegalStateException("Could not list files in " + dir.getAbsolutePath());
        for (File file : files) {
            try (ZipInputStream zis = new ZipInputStream(new FileInputStream(file))) {
                ZipEntry entry;

                while ((entry = zis.getNextEntry()) != null) {
                    if (!entry.isDirectory()) {
                        String name = entry.getName();
                        if (name.endsWith(".track.xml")) {
                            String track_id = name.substring(0, name.length() - ".track.xml".length());
                            sourceFiles.put(track_id, file.toPath());
                        }

                        if (name.endsWith(".environment.xml")) {
                            byte[] bytes = zis.readAllBytes();

                            EnvironmentFile environmentFile = TrackLoader.loadEnvironmentFile(new ByteArrayInputStream(bytes));

                            // preload all the environments
                            TrackInstance.getDimensionKey(environmentFile);
                        }
                    }

                    zis.closeEntry();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return List.copyOf(sourceFiles.keySet());
    }

    @Override
    public Optional<Track> getTrack(String track_id) {

        Path file = sourceFiles.get(track_id);

        if (file == null) {
            log.warn("Failed to fetch uncached track {}.", track_id);
            return Optional.empty();
        }

        Track cached = tracks.get(track_id);

        if (cached != null) {
            return Optional.of(cached);
        }

        PolarLoader world = null;
        EnvironmentFile environmentFile = null;
        List<TrackFile> trackFiles = new ArrayList<>();

        String env_name = null;

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(file.toFile()))) {
            ZipEntry entry;

            while ((entry = zis.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    byte[] bytes = zis.readAllBytes();

                    if (entry.getName().endsWith(".polar")) {
                        world = new PolarLoader(new ByteArrayInputStream(bytes));
                        env_name = entry.getName().substring(0, entry.getName().length() - ".polar".length());
                    } else if (entry.getName().endsWith(".environment.xml")) {
                        environmentFile = TrackLoader.loadEnvironmentFile(new ByteArrayInputStream(bytes));
                    } else if (entry.getName().endsWith(".track.xml")) {
                        trackFiles.add(TrackLoader.loadTrack(new ByteArrayInputStream(bytes)));
                    }
                }

                zis.closeEntry();
            }
        } catch (IOException e) {
            log.error("Failed to load stomtrack from file", e);
            return Optional.empty();
        }

        if (trackFiles.isEmpty()) {
            log.warn("Track file has no tracks! {}", file);
            return Optional.empty();
        }

        if (world == null) {
            log.warn("Track file has no world! {}", file);
            return Optional.empty();
        }

        if (environmentFile == null) {
            log.warn("Track file has no environment data! {}", file);
            return Optional.empty();
        }

        Track source = null;

        for (TrackFile trackFile : trackFiles) {
            Track track = new Track(
                    trackFile,
                    world.world(),
                    environmentFile,
                    env_name
            );

            tracks.put(trackFile.getId(), track);

            if (trackFile.getId().equals(track_id)) {
                source = track;
            }
        }

        return Optional.ofNullable(source);
    }
}
