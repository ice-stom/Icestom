package io.gitlab.icestom.icestom.track.format;


import com.moandjiezana.toml.Toml;
import com.moandjiezana.toml.TomlWriter;
import io.gitlab.icestom.icestom.track.Track;
import net.hollowcube.polar.PolarReader;
import net.hollowcube.polar.PolarWorld;
import net.hollowcube.polar.PolarWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class StomtrackFormat {

    public static final String FILE_EXTENSION = "stomtrack";

    private static final TomlWriter tomlWriter = new TomlWriter.Builder()
            .build();

    public static class TrackLoadException extends Exception {
        TrackLoadException(String message) {
            super(message);
        }
    }

    public static class TrackSaveException extends Exception {
        TrackSaveException(String message) {
            super(message);
        }
    }

    public static List<Track> loadStomtrack(File trackfile) throws IOException, TrackLoadException {

        PolarWorld polarWorld = null;
        TrackEnvironmentData environmentData = null;
        String worldId = null;

        List<TrackData> tracks = new ArrayList<>();

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(trackfile))) {
            ZipEntry entry = zis.getNextEntry();
            while (entry != null) {
                String name = entry.getName();
                String ext = name.substring(name.lastIndexOf(".") + 1);

                switch (ext) {
                    case "toml": {
                        byte[] bytes = zis.readAllBytes();

                        String data = new String(bytes, StandardCharsets.UTF_8);

                        tracks.add(TrackData.deserialize(
                                new Toml().read(data)
                        ));

                        break;
                    }
                    case "environment": {
                        if (environmentData != null) throw new TrackLoadException("More than one environment data!");
                        byte[] bytes = zis.readAllBytes();

                        String data = new String(bytes, StandardCharsets.UTF_8);

                        environmentData = new Toml().read(data).to(TrackEnvironmentData.class);

                        break;
                    }
                    case "polar": {
                        if (polarWorld != null) throw new TrackLoadException("More than one world data!");

                        polarWorld = PolarReader.read(zis.readAllBytes());
                        worldId = name.substring(0, name.lastIndexOf("."));
                        break;
                    }
                }

                entry = zis.getNextEntry();
            }
        } catch (TrackData.TrackDeserializationException e) {
            throw new TrackLoadException("Failed to deserialize track: " + e.getMessage());
        }

        if (polarWorld == null) throw new TrackLoadException("Failed to load world data");
        if (environmentData == null) throw new TrackLoadException("Failed to load environment data");

        // cache this while registry isn't frozen
        environmentData.getKey();

        final PolarWorld finalWorld = polarWorld;
        final String finalWorldId = worldId;
        final TrackEnvironmentData finalEnvironmentData = environmentData;
        return tracks
                .stream()
                .map(trackData -> new Track(trackData, finalWorld, finalEnvironmentData, finalWorldId))
                .toList();
    }

    public static void saveStomtrack(File trackfile, PolarWorld world, List<Track> tracks) throws IOException, TrackSaveException {

        if (tracks.isEmpty()) throw new TrackSaveException("");

        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(trackfile))) {

            for (Track track : tracks) {
                ZipEntry manifest = new ZipEntry(track.getId() + ".toml");
                zos.putNextEntry(manifest);
                tomlWriter.write(track.serialize(), zos);
                zos.flush();
            }

            byte[] data = PolarWriter.write(world);

            ZipEntry worldEntry = new ZipEntry("spawn.polar");
            zos.putNextEntry(worldEntry);
            zos.write(data);
            zos.flush();
        }
    }
}