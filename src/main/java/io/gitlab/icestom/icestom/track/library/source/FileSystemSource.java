package io.gitlab.icestom.icestom.track.library.source;

import io.gitlab.icestom.icestom.IceStom;
import io.gitlab.icestom.icestom.track.Track;
import io.gitlab.icestom.icestom.track.TrackLoad;
import io.gitlab.icestom.icestom.track.library.VirtualTrackInstance;
import io.gitlab.icestom.stomtrack.EnvironmentFile;
import io.gitlab.icestom.stomtrack.TrackFile;
import io.gitlab.icestom.stomtrack.TrackLoader;
import net.hollowcube.polar.PolarDataConverter;
import net.hollowcube.polar.PolarLoader;
import net.hollowcube.polar.PolarWorldAccess;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
import net.minestom.server.instance.Chunk;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.network.NetworkBuffer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URI;
import java.nio.channels.Channels;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import static io.gitlab.icestom.icestom.track.Track.getDimensionKey;
import static io.gitlab.icestom.icestom.util.DisplayEntityConverter.*;

public class FileSystemSource extends TrackSource {

    private static final Logger log = LoggerFactory.getLogger(FileSystemSource.class);
    private final Path folder;

    private final Map<String, Path> sourceFiles = new LinkedHashMap<>();

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
                            Track.getDimensionKey(environmentFile);
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
    public @NotNull CompletableFuture<Track> loadTrack(String track_id) {

        Path file = sourceFiles.get(track_id);

        if (file == null) {
            log.warn("Failed to fetch unindexed track {}.", track_id);
            return CompletableFuture.failedFuture(new RuntimeException("Failed to fetch unindexed track"));
        }

        return CompletableFuture.supplyAsync(() -> {
            EnvironmentFile environmentFile = null;
            List<TrackFile> trackFiles = new ArrayList<>();

            String env_name = null;

            ZipFile zipFile;
            try {
                zipFile = new ZipFile(file.toFile());

                Enumeration<? extends ZipEntry> entries = zipFile.entries();

                String polar_entry = null;

                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();

                    if (entry.getName().endsWith(".environment.xml")) {
                        environmentFile = TrackLoader.loadEnvironmentFile(zipFile.getInputStream(entry));
                    } else if (entry.getName().endsWith(".track.xml")) {
                        trackFiles.add(TrackLoader.loadTrack(zipFile.getInputStream(entry)));
                    } else if (entry.getName().endsWith(".polar")) {
                        polar_entry = entry.getName();
                        env_name = polar_entry.substring(0, polar_entry.length() - ".polar".length());
                    }
                }

                if (trackFiles.isEmpty()) {
                    throw new RuntimeException("Track file has no tracks!");
                }

                if (environmentFile == null) {
                    throw new RuntimeException("Track file has no environment data!");
                }

                if (env_name == null) {
                    throw new RuntimeException("Track file has no world!");
                }

                ZipEntry zipEntry = zipFile.getEntry(polar_entry);

                TrackLoad trackLoad = new PolarTrackLoad(
                        environmentFile,
                        env_name,
                        zipFile.getInputStream(zipEntry),
                        zipEntry.getSize()
                );

                trackLoad.fullyLoaded().thenRun(() -> {
                    try {
                        zipFile.close();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });

                Track source = null;

                for (TrackFile trackFile : trackFiles) {
                    Track track = new Track(
                            trackFile,
                            trackLoad
                    );

                    if (trackFile.getId().equals(track_id)) {
                        source = track;
                    }
                }

                if (source == null) {
                    log.error("Failed to locate {} in {}", track_id, file);
                    throw new RuntimeException("Failed to locate track in file.");
                }

                trackLoad.spawnLoaded().join();

                return source;
            } catch (IOException e) {
                log.error("Failed to load stomtrack from file", e);
                throw new RuntimeException("Failed to load stomtrack from file");
            }
        });
    }

    public static class PolarTrackLoad implements TrackLoad {

        private final VirtualTrackInstance instanceContainer;

        private final CompletableFuture<Void> fullyLoaded;

        @SuppressWarnings("UnstableApiUsage")
        public PolarTrackLoad(EnvironmentFile environmentFile, String env_name, InputStream inputStream, long bytes) {
            instanceContainer = new VirtualTrackInstance(env_name, environmentFile);

            fullyLoaded = PolarLoader.streamLoad(
                    instanceContainer,
                    Channels.newChannel(inputStream),
                    bytes,
                    PolarDataConverter.NOOP,
                    new PolarWorldAccess() {
                        boolean hasEntityData = true;

                        @Override
                        public void loadChunkData(@NotNull Chunk chunk, @Nullable NetworkBuffer userData) {
                            if (!hasEntityData) return;
                            if (userData == null) return;

                            try {
                                CompoundBinaryTag root = userData.read(NetworkBuffer.NBT_COMPOUND);
                                ListBinaryTag list = root.getList("entities");

                                for (BinaryTag binaryTag : list) {
                                    instanceContainer.addDisplayEntity((CompoundBinaryTag) binaryTag);
                                }
                            } catch (IndexOutOfBoundsException e) {
                                hasEntityData = false;
                            }
                        }
                    },
                    true
            ).exceptionally(throwable -> {
                log.error("Failed track load!", throwable);
                return null;
            });
        }

        @Override
        public VirtualTrackInstance instanceContainer() {
            return instanceContainer;
        }

        @Override
        public CompletableFuture<Void> spawnLoaded() {
            return fullyLoaded; // make this actually check for spawn to be loaded :thumbs_up:
        }

        @Override
        public CompletableFuture<Void> fullyLoaded() {
            return fullyLoaded;
        }
    }
}
