package io.gitlab.icestom.linefold;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LZ4PositionWriterTest {

    private record Packet(int streamId, double x, double y, double z, float yaw) {}
    private record SinglePacket(double x, double y, double z, float yaw) {}

    private static List<Packet> readPackets(byte[] bytes) throws IOException {
        List<Packet> read = new ArrayList<>();
        LZ4PositionReader.readAll(new ByteArrayInputStream(bytes), (streamId, x, y, z, yaw) ->
                read.add(new Packet(streamId, x, y, z, yaw)));
        return read;
    }

    @Test
    void roundTripsPacketsAcrossMultipleStreams() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        List<Packet> written = new ArrayList<>();
        try (LZ4PositionWriter writer = new LZ4PositionWriter(out)) {
            for (int i = 0; i < 500; i++) {
                int streamId = i % 3;
                double x = i * 1.5;
                double y = i * -2.25;
                double z = Math.sqrt(i + 1);
                float yaw = (i * 0.75f) - 180f;
                writer.writePacket(streamId, x, y, z, yaw);
                written.add(new Packet(streamId, x, y, z, yaw));
            }
            writer.flush();
        }

        List<Packet> read = readPackets(out.toByteArray());

        assertEquals(written.size(), read.size());
        for (int i = 0; i < written.size(); i++) {
            assertEquals(written.get(i), read.get(i), "mismatch at packet index " + i);
        }
    }

    @Test
    void roundTripsSingleStreamPackets() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        List<SinglePacket> written = new ArrayList<>();
        try (LZ4SinglePositionWriter writer = new LZ4SinglePositionWriter(out)) {
            for (int i = 0; i < 300; i++) {
                double x = i * 0.1;
                double y = i * -0.2;
                double z = i * 0.3;
                float yaw = i * 1.25f;
                writer.writePacket(x, y, z, yaw);
                written.add(new SinglePacket(x, y, z, yaw));
            }
        }

        List<SinglePacket> read = new ArrayList<>();
        LZ4SinglePositionReader.readAll(new ByteArrayInputStream(out.toByteArray()), (x, y, z, yaw) ->
                read.add(new SinglePacket(x, y, z, yaw)));

        assertEquals(written.size(), read.size());
        for (int i = 0; i < written.size(); i++) {
            assertEquals(written.get(i), read.get(i), "single stream mismatch at index " + i);
        }
    }

    @Test
    void roundTripsThroughFileStreams(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("positions.lz4");

        List<Packet> written = new ArrayList<>();
        try (LZ4PositionWriter writer = new LZ4PositionWriter(Files.newOutputStream(file))) {
            for (int i = 0; i < 200; i++) {
                writer.writePacket(i % 2, i * 0.5, i * 1.5, i * 2.5, i * 0.1f);
                written.add(new Packet(i % 2, i * 0.5, i * 1.5, i * 2.5, i * 0.1f));
            }
        }

        List<Packet> read = new ArrayList<>();
        try (InputStream in = Files.newInputStream(file)) {
            LZ4PositionReader.readAll(in, (streamId, x, y, z, yaw) ->
                    read.add(new Packet(streamId, x, y, z, yaw)));
        }

        assertEquals(written, read);
    }

    @Test
    void smallBufferForcesMultipleFrames() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        List<Packet> written = new ArrayList<>();
        // 40 bytes holds exactly one worst-case record, so nearly every packet swaps buffers
        try (LZ4PositionWriter writer = new LZ4PositionWriter(out, 40)) {
            for (int i = 0; i < 100; i++) {
                writer.writePacket(i % 4, i * 3.7, i * -1.1, i * 0.9, i * -0.7f);
                written.add(new Packet(i % 4, i * 3.7, i * -1.1, i * 0.9, i * -0.7f));
            }
        }

        assertEquals(written, readPackets(out.toByteArray()));
    }

    @Test
    void writePacketAfterCloseThrows() throws IOException {
        LZ4PositionWriter writer = new LZ4PositionWriter(new ByteArrayOutputStream());
        writer.writePacket(1, 1.0, 2.0, 3.0, 4.0f);
        writer.close();

        assertThrows(IllegalStateException.class, () -> writer.writePacket(1, 4.0, 5.0, 6.0, 7.0f));
    }

    @Test
    void emptyStreamRoundTrip() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (LZ4PositionWriter writer = new LZ4PositionWriter(out)) {
            // Just close without writing packets
        }

        assertEquals(0, readPackets(out.toByteArray()).size());
    }

    @Test
    void closeClosesUnderlyingStream() throws IOException {
        AtomicBoolean closed = new AtomicBoolean();
        OutputStream tracking = new ByteArrayOutputStream() {
            @Override
            public void close() throws IOException {
                closed.set(true);
                super.close();
            }
        };

        LZ4PositionWriter writer = new LZ4PositionWriter(tracking);
        writer.close();

        assertTrue(closed.get(), "underlying stream should be closed with the writer");
    }

    @Test
    void invalidBufferCapacityThrowsAndClosesStream() {
        AtomicBoolean closed = new AtomicBoolean();
        OutputStream tracking = new ByteArrayOutputStream() {
            @Override
            public void close() throws IOException {
                closed.set(true);
                super.close();
            }
        };

        // Capacity smaller than MAX_RECORD_BYTES (40) should fail
        assertThrows(IllegalArgumentException.class, () -> new LZ4PositionWriter(tracking, 10));
        assertTrue(closed.get(), "stream should not leak when construction fails");
    }

    @Test
    void nullStreamThrows() {
        assertThrows(NullPointerException.class, () -> new LZ4PositionWriter((OutputStream) null));
    }

    @Test
    void concurrentMultiStreamWrites() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int threadCount = 4;
        int packetsPerThread = 500;

        try (LZ4PositionWriter writer = new LZ4PositionWriter(out)) {
            Thread[] threads = new Thread[threadCount];
            for (int t = 0; t < threadCount; t++) {
                final int streamId = t;
                threads[t] = new Thread(() -> {
                    for (int i = 0; i < packetsPerThread; i++) {
                        writer.writePacket(streamId, i * 1.1, i * 2.2, i * 3.3, i * 0.5f);
                    }
                });
                threads[t].start();
            }

            for (Thread t : threads) {
                t.join();
            }
        }

        assertEquals(threadCount * packetsPerThread, readPackets(out.toByteArray()).size());
    }

    @Test
    void yawPreservesExactFloatBitsIncludingSpecialValues() throws IOException {
        float[] yaws = {
                0.0f, -0.0f, 1.0f, -1.0f, 179.99999f, -180f, 360f,
                Float.MIN_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE,
                Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NaN
        };

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (LZ4PositionWriter writer = new LZ4PositionWriter(out)) {
            for (float yaw : yaws) {
                writer.writePacket(7, 1.0, 2.0, 3.0, yaw);
            }
        }

        List<Packet> read = readPackets(out.toByteArray());
        assertEquals(yaws.length, read.size());
        for (int i = 0; i < yaws.length; i++) {
            assertEquals(Float.floatToRawIntBits(yaws[i]), Float.floatToRawIntBits(read.get(i).yaw()),
                    "yaw bits differ at index " + i);
        }
    }

    @Test
    void singleStreamYawPreservesExactFloatBitsIncludingSpecialValues() throws IOException {
        float[] yaws = {0.0f, -0.0f, 90.5f, -90.5f, Float.MAX_VALUE, Float.NaN, Float.NEGATIVE_INFINITY};

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (LZ4SinglePositionWriter writer = new LZ4SinglePositionWriter(out)) {
            for (float yaw : yaws) {
                writer.writePacket(1.0, 2.0, 3.0, yaw);
            }
        }

        List<Float> read = new ArrayList<>();
        LZ4SinglePositionReader.readAll(new ByteArrayInputStream(out.toByteArray()), (x, y, z, yaw) -> read.add(yaw));

        assertEquals(yaws.length, read.size());
        for (int i = 0; i < yaws.length; i++) {
            assertEquals(Float.floatToRawIntBits(yaws[i]), Float.floatToRawIntBits(read.get(i)),
                    "single stream yaw bits differ at index " + i);
        }
    }

    @Test
    void yawIsDeltaEncodedIndependentlyPerStream() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        List<Packet> written = new ArrayList<>();
        try (LZ4PositionWriter writer = new LZ4PositionWriter(out)) {
            // Interleave two streams with very different yaw values so a shared
            // delta baseline would corrupt one of them.
            for (int i = 0; i < 100; i++) {
                float yawA = 10f + i * 0.5f;
                float yawB = -170f - i * 0.25f;
                writer.writePacket(0, i, i, i, yawA);
                writer.writePacket(1, -i, -i, -i, yawB);
                written.add(new Packet(0, i, i, i, yawA));
                written.add(new Packet(1, -i, -i, -i, yawB));
            }
        }

        assertEquals(written, readPackets(out.toByteArray()));
    }

    @Test
    void worstCaseRecordFitsInMinimumBuffer() throws IOException {
        // Alternating bit patterns maximise every XOR delta, so each field takes
        // its maximum varint length. The record must still fit in MAX_RECORD_BYTES (40).
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        List<Packet> written = new ArrayList<>();
        try (LZ4PositionWriter writer = new LZ4PositionWriter(out, 40)) {
            for (int i = 0; i < 50; i++) {
                boolean flip = (i % 2) == 0;
                int streamId = Integer.MAX_VALUE; // 5-byte varint
                double x = flip ? -Double.MAX_VALUE : Double.MIN_VALUE;
                double y = flip ? -1.0e300 : 1.0e-300;
                double z = flip ? -Double.MAX_VALUE : 1.0;
                float yaw = flip ? -Float.MAX_VALUE : Float.MIN_VALUE;
                writer.writePacket(streamId, x, y, z, yaw);
                written.add(new Packet(streamId, x, y, z, yaw));
            }
        }

        assertEquals(written, readPackets(out.toByteArray()));
    }
}
