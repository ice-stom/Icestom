package io.gitlab.icestom.icestom.track;


import io.gitlab.icestom.icestom.track.checkpoint.Checkpoint;
import net.hollowcube.polar.PolarReader;
import net.hollowcube.polar.PolarWorld;
import net.hollowcube.polar.PolarWriter;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class TrackFormat {

    public static final String FILE_EXTENTION = "stomtrack";

    private static final Logger log = LoggerFactory.getLogger(TrackFormat.class);

    private static final Map<Class<?>, Character> object_prefixes = Map.of(
            Byte.class, 'B',
            Integer.class, 'I',
            Long.class, 'L',
            Float.class, 'F',
            Double.class, 'D',
            String.class, 'S'
    );

    public static class TrackLoadException extends Exception {
        TrackLoadException(String message) {
            super(message);
        }
    }

    public static class Writer extends ZipOutputStream {
        public Writer(@NotNull OutputStream out) {
            super(out);
        }

        public void writeString(String s) throws IOException {
            byte[] bytes = s.getBytes();
            write(bytes);
        }

        public void writeKV(String k, Object v) throws IOException {
            char object_prefix = object_prefixes.getOrDefault(v.getClass(), 'O');

            writeString(k);
            write('=');
            write(object_prefix);
            write(';');
            writeString(v.toString());
            write('\n');
        }

        public void writePos(Pos pos) throws IOException {
            writeString(Double.toString(pos.x()));
            write(',');
            writeString(Double.toString(pos.y()));
            write(',');
            writeString(Double.toString(pos.z()));
            write(',');
            writeString(Double.toString(pos.yaw()));
            write(',');
            writeString(Double.toString(pos.pitch()));
            write(' ');
        }

        public void writeVec(Vec pos) throws IOException {
            writeString(Double.toString(pos.x()));
            write(',');
            writeString(Double.toString(pos.y()));
            write(',');
            writeString(Double.toString(pos.z()));
            write(' ');
        }

        public void writeTrack(@NotNull TrackData track) throws IOException {
            writeString(FILE_EXTENTION.toUpperCase() + '\n');
            writeKV("id", track.getId());
            write('\n');
            write('\n');


            writeString("spawn_location");
            write(':');
            writePos(track.getSpawnLocation());
            write('\n');

            write('\n');

            for (Map.Entry<Checkpoint, Integer> checkpoint : track.getCheckpoints().entrySet()) {
                writeString(checkpoint.getKey().getClass().getSimpleName());
                write(':');
                writeString(Integer.toString(checkpoint.getValue()));
                write(':');
                checkpoint.getKey().write(this);
                write('\n');
            }
        }
    }

    public static class Reader extends ZipInputStream {
        public Reader(@NotNull InputStream in) {
            super(in);
        }

        public String readString(int length) throws IOException {
            byte[] bytes = new byte[length];
            read(bytes);
            return new String(bytes);
        }

        public Map.Entry<String, Object> readKV() throws IOException {
            StringBuilder key = new StringBuilder();
            int b;

            while ((b = read()) != '=') {
                key.append((char) b);
            }

            char objectPrefix = (char) read();
            read();

            Class<?> type = Object.class;
            for (Map.Entry<Class<?>, Character> e : object_prefixes.entrySet()) {
                if (e.getValue() == objectPrefix) {
                    type = e.getKey();
                    break;
                }
            }

            StringBuilder value = new StringBuilder();
            while ((b = read()) != '\n') {
                value.append((char) b);
            }

            Object parsed = parseValue(value.toString(), type);
            return Map.entry(key.toString(), parsed);
        }

        public Pos readPos() throws IOException {
            double[] parts = new double[5]; // x, y, z, yaw, pitch
            for (int i = 0; i < 5; i++) {
                StringBuilder sb = new StringBuilder();
                int b;
                char delimiter = (i < 4) ? ',' : ' ';
                while ((b = read()) != delimiter) {
                    sb.append((char) b);
                }
                parts[i] = Double.parseDouble(sb.toString());
            }
            return new Pos(parts[0], parts[1], parts[2], (float) parts[3], (float) parts[4]);
        }

        public Vec readVec() throws IOException {
            double[] parts = new double[3]; // x, y, z
            for (int i = 0; i < 3; i++) {
                StringBuilder sb = new StringBuilder();
                int b;
                char delimiter = (i < 2) ? ',' : ' ';
                while ((b = read()) != delimiter) {
                    sb.append((char) b);
                }
                parts[i] = Double.parseDouble(sb.toString());
            }
            return new Vec(parts[0], parts[1], parts[2]);
        }

        public TrackData readTrack() throws IOException {
            MutableTrack track = new MutableTrack();

            StringBuilder header = new StringBuilder();
            int b;
            while ((b = read()) != '\n') {
                header.append((char) b);
            }
            if (!header.toString().equals(FILE_EXTENTION.toUpperCase())) {
                throw new IOException("Invalid track file header: " + header);
            }

            Map.Entry<String, Object> idEntry = readKV();
            if (!idEntry.getKey().equals("id")) {
                throw new IOException("Expected 'id' key, got: " + idEntry.getKey());
            }
            track.setId(idEntry.getValue().toString());

            read(); // consume first blank '\n'
            read(); // consume second blank '\n'

            String locationLabel = readUntil(':', "spawn location label");
            if (!locationLabel.equals("spawn_location")) {
                throw new IOException("Expected 'spawn_location' label, got: " + locationLabel);
            }
            Pos spawnLocation = readPos();
            track.setSpawnLocation(spawnLocation);

            read(); // consume '\n' after spawn_location line
            read(); // consume blank '\n' separator

            Map<Checkpoint, Integer> checkpoints = new LinkedHashMap<>();

            Checkpoint.IndexedCheckpoint checkpoint;
            while ((checkpoint = Checkpoint.read(this)) != null) {
                checkpoints.put(checkpoint.checkpoint(), checkpoint.index());
            }

            track.setCheckpoints(checkpoints);

            return track;
        }

        public String readUntil(char delimiter, String fieldName) throws IOException {
            StringBuilder sb = new StringBuilder();
            int b;
            while ((b = read()) != delimiter) {
                if (b == -1) throw new IOException("Unexpected EOF while reading " + fieldName);
                sb.append((char) b);
            }
            return sb.toString();
        }

        public String readUntilOrEof(char delimiter) throws IOException {
            StringBuilder sb = new StringBuilder();
            int b;
            while ((b = read()) != delimiter) {
                if (b == -1) return null;
                sb.append((char) b);
            }
            return sb.toString();
        }

        private Object parseValue(String value, Class<?> type) {
            if (type == Integer.class) return Integer.parseInt(value);
            if (type == Double.class) return Double.parseDouble(value);
            if (type == Boolean.class) return Boolean.parseBoolean(value);
            if (type == Long.class) return Long.parseLong(value);
            return value;
        }
    }

    public static Track loadTrack(File trackfile) throws IOException, TrackLoadException {

        TrackData trackData = null;
        PolarWorld polarWorld = null;

        try (Reader zis = new Reader(new FileInputStream(trackfile))) {
            ZipEntry entry = zis.getNextEntry();
            while (entry != null) {
                switch (entry.getName()) {
                    case "manifest": {
                        trackData = zis.readTrack();
                        break;
                    }
                    case "world.polar": {
                        polarWorld = PolarReader.read(zis.readAllBytes());
                        break;
                    }
                }

                entry = zis.getNextEntry();
            }
        }

        if (trackData == null) throw new TrackLoadException("Failed to load manifest");

        return new Track(trackData, polarWorld);
    }

    public static void saveTrack(File trackfile, Track track) throws IOException {
        try (Writer writer = new TrackFormat.Writer(new FileOutputStream(trackfile))) {
            ZipEntry manifest = new ZipEntry("manifest");
            writer.putNextEntry(manifest);
            writer.writeTrack(track);
            writer.flush();

            byte[] data = PolarWriter.write(track.getWorld());

            ZipEntry world = new ZipEntry("world.polar");
            writer.putNextEntry(world);
            writer.write(data);
            writer.flush();
        }
    }
}