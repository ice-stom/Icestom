package io.gitlab.icestom.linefold;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LZ4PositionWriterTest {

    private record Packet(int streamId, double x, double y, double z) {}

    @Test
    void roundTripsPacketsAcrossMultipleStreams(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("positions.lz4");

        List<Packet> written = new ArrayList<>();
        try (LZ4PositionWriter writer = new LZ4PositionWriter(file)) {
            for (int i = 0; i < 500; i++) {
                int streamId = i % 3;
                double x = i * 1.5;
                double y = i * -2.25;
                double z = Math.sqrt(i + 1);
                writer.writePacket(streamId, x, y, z);
                written.add(new Packet(streamId, x, y, z));
            }
            writer.flush();
        }

        List<Packet> read = new ArrayList<>();
        LZ4PositionReader.readAll(file, (streamId, x, y, z) ->
                read.add(new Packet(streamId, x, y, z)));

        assertEquals(written.size(), read.size());
        for (int i = 0; i < written.size(); i++) {
            assertEquals(written.get(i), read.get(i), "mismatch at packet index " + i);
        }
    }

    @Test
    void writePacketAfterCloseThrows(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("closed.lz4");
        LZ4PositionWriter writer = new LZ4PositionWriter(file);
        writer.writePacket(1, 1.0, 2.0, 3.0);
        writer.close();

        assertThrows(IllegalStateException.class, () -> writer.writePacket(1, 4.0, 5.0, 6.0));
    }
}