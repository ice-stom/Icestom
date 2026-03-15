package io.gitlab.icestom.icestom.track.format;


import com.moandjiezana.toml.Toml;
import com.moandjiezana.toml.TomlWriter;
import io.gitlab.icestom.icestom.track.Track;
import net.hollowcube.polar.PolarReader;
import net.hollowcube.polar.PolarWorld;
import net.hollowcube.polar.PolarWriter;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class TrackFormat {

    public static final String FILE_EXTENTION = "stomtrack";

    private static final TomlWriter tomlWriter = new TomlWriter.Builder()
            .build();

    public static class TrackLoadException extends Exception {
        TrackLoadException(String message) {
            super(message);
        }
    }

    public static Track loadTrack(File trackfile) throws IOException, TrackLoadException {

        TrackData trackData = null;
        PolarWorld polarWorld = null;

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(trackfile))) {
            ZipEntry entry = zis.getNextEntry();
            while (entry != null) {
                switch (entry.getName()) {
                    case "manifest": {
                        byte[] bytes = zis.readAllBytes();
                        trackData = TrackData.deserialize(new Toml().read(new String(bytes, StandardCharsets.UTF_8)));
                        break;
                    }
                    case "world.polar": {
                        polarWorld = PolarReader.read(zis.readAllBytes());
                        break;
                    }
                }

                entry = zis.getNextEntry();
            }
        } catch (TrackData.TrackDeserializationException e) {
            throw new TrackLoadException("Failed to deserialize track: " + e.getMessage());
        }

        if (trackData == null) throw new TrackLoadException("Failed to load manifest");

        return new Track(trackData, polarWorld);
    }

    public static void saveTrack(File trackfile, Track track) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(trackfile))) {
            ZipEntry manifest = new ZipEntry("manifest");
            zos.putNextEntry(manifest);
            tomlWriter.write(track.serialize(), zos);
            zos.flush();

            byte[] data = PolarWriter.write(track.getWorld());

            ZipEntry world = new ZipEntry("world.polar");
            zos.putNextEntry(world);
            zos.write(data);
            zos.flush();
        }
    }
}