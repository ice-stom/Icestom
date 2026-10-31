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
    private record SinglePacket(double x, double y, double z) {}

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
    void roundTripsSingleStreamPackets(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("single.lz4");

        List<SinglePacket> written = new ArrayList<>();
        try (LZ4SinglePositionWriter writer = new LZ4SinglePositionWriter(file)) {
            for (int i = 0; i < 300; i++) {
                double x = i * 0.1;
                double y = i * -0.2;
                double z = i * 0.3;
                writer.writePacket(x, y, z);
                written.add(new SinglePacket(x, y, z));
            }
        }

        List<SinglePacket> read = new ArrayList<>();
        LZ4SinglePositionReader.readAll(file, (x, y, z) ->
                read.add(new SinglePacket(x, y, z)));

        assertEquals(written.size(), read.size());
        for (int i = 0; i < written.size(); i++) {
            assertEquals(written.get(i), read.get(i), "single stream mismatch at index " + i);
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

    @Test
    void emptyFileRoundTrip(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("empty.lz4");
        try (LZ4PositionWriter writer = new LZ4PositionWriter(file)) {
            // Just close without writing packets
        }

        List<Packet> read = new ArrayList<>();
        LZ4PositionReader.readAll(file, (streamId, x, y, z) ->
                read.add(new Packet(streamId, x, y, z)));

        assertEquals(0, read.size());
    }

    @Test
    void invalidBufferCapacityThrows(@TempDir Path tempDir) {
        Path file = tempDir.resolve("invalid_buf.lz4");
        // Capacity smaller than MAX_RECORD_BYTES (40) should fail
        assertThrows(IllegalArgumentException.class, () -> new LZ4PositionWriter(file, 10));
    }

    @Test
    void concurrentMultiStreamWrites(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("concurrent.lz4");
        int threadCount = 4;
        int packetsPerThread = 500;

        try (LZ4PositionWriter writer = new LZ4PositionWriter(file)) {
            Thread[] threads = new Thread[threadCount];
            for (int t = 0; t < threadCount; t++) {
                final int streamId = t;
                threads[t] = new Thread(() -> {
                    for (int i = 0; i < packetsPerThread; i++) {
                        writer.writePacket(streamId, i * 1.1, i * 2.2, i * 3.3);
                    }
                });
                threads[t].start();
            }

            for (Thread t : threads) {
                t.join();
            }
        }

        List<Packet> read = new ArrayList<>();
        LZ4PositionReader.readAll(file, (streamId, x, y, z) ->
                read.add(new Packet(streamId, x, y, z)));

        assertEquals(threadCount * packetsPerThread, read.size());
    }
}