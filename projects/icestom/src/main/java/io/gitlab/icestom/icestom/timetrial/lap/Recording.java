package io.gitlab.icestom.icestom.timetrial.lap;

import io.gitlab.icestom.linefold.LZ4SinglePositionWriter;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

public class Recording {

    private final ByteArrayOutputStream buffer;
    private final LZ4SinglePositionWriter writer;

    public Recording() {
        buffer = new ByteArrayOutputStream();
        writer = new LZ4SinglePositionWriter(buffer);
    }

    public void writePacket(double x, double y, double z, float yaw) {
        writer.writePacket(x, y, z, yaw);
    }

    public void dump(Path path) throws IOException {
        writer.flush();
        try (OutputStream file = Files.newOutputStream(path)) {
            buffer.writeTo(file);
        }
    }

    public ByteArrayOutputStream getBuffer() {
        return buffer;
    }

    public void discard() throws IOException {
        writer.close();
        buffer.close();
    }
}
