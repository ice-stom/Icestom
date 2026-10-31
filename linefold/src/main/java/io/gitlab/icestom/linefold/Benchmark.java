package io.gitlab.icestom.linefold;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public class Benchmark {

    private record Packet(int streamId, double x, double y, double z) {}

    private record Result(String name, long bytes, long nanos) {
        double nanosPerPacket(int count) {
            return count == 0 ? 0 : (double) nanos / count;
        }

        double packetsPerSecond(int count) {
            return nanos == 0 ? 0 : count * 1_000_000_000.0 / nanos;
        }
    }

    static void main(String[] args) throws Exception {
        int count = args.length > 0
                ? Integer.parseInt(args[0])
                : 1_000_000;

        long seed = 123456789L;

        Path dir = Files.createTempDirectory("position-benchmark-");

        Path lz4 = dir.resolve("positions.lz4");
        Path json = dir.resolve("positions.jsonl");
        Path binary = dir.resolve("positions.bin");

        System.out.println("Generating " + count + " packets using a pool of 100 random offsets...");
        System.out.println("Random seed: " + seed);

        List<Packet> packets = generatePackets(count, seed);

        // Warm up the JVM and compression implementation.
        // Uses a separate file to avoid affecting measured output.
        Path warmup = dir.resolve("warmup.lz4");

        try (LZ4PositionWriter writer = new LZ4PositionWriter(warmup)) {
            for (int i = 0; i < Math.min(count, 10_000); i++) {
                Packet p = packets.get(i);
                writer.writePacket(p.streamId(), p.x(), p.y(), p.z());
            }
        }

        Files.deleteIfExists(warmup);

        List<Result> results = new ArrayList<>();

        results.add(benchmarkLZ4(lz4, packets));
        results.add(benchmarkJSON(json, packets));
        results.add(benchmarkBinary(binary, packets));

        System.out.println();
        System.out.println("==============================================");
        System.out.println("POSITION ENCODING BENCHMARK");
        System.out.println("==============================================");
        System.out.println("Packets: " + count);
        System.out.println("Seed:    " + seed);
        System.out.println();

        System.out.printf(
                "%-12s %14s %12s %14s %16s%n",
                "Format",
                "Size (bytes)",
                "Bytes/pkt",
                "Time (ms)",
                "Packets/sec"
        );

        System.out.println(
                "--------------------------------------------------------------------------"
        );

        for (Result r : results) {
            System.out.printf(
                    "%-12s %14d %12.2f %14.3f %16.0f%n",
                    r.name(),
                    r.bytes(),
                    (double) r.bytes() / count,
                    r.nanos() / 1_000_000.0,
                    r.packetsPerSecond(count)
            );
        }

        System.out.println();
        System.out.println("Average encoding time per packet:");

        for (Result r : results) {
            System.out.printf(
                    "%-12s %12.2f ns/packet%n",
                    r.name(),
                    r.nanosPerPacket(count)
            );
        }

        long binarySize = results.get(2).bytes();

        System.out.println();
        System.out.println("Relative file sizes (raw binary = 100%):");

        for (Result r : results) {
            System.out.printf(
                    "%-12s %8.2f%%%n",
                    r.name(),
                    binarySize == 0
                            ? 0
                            : 100.0 * r.bytes() / binarySize
            );
        }

        System.out.println();
        System.out.println("Output directory: " + dir);
    }

    private static List<Packet> generatePackets(int count, long seed) {
        Random random = new Random(seed);
        List<Packet> packets = new ArrayList<>(count);

        // Pre-generate a list of 100 random offsets
        double[] offsetPool = new double[2000];
        for (int i = 0; i < 2000; i++) {
            offsetPool[i] = random.nextDouble(-0.5, 0.5);
        }

        // Simulate 3 independent streams (matching streamId = i % 3)
        double[][] streamPositions = new double[3][3];
        for (int s = 0; s < 3; s++) {
            streamPositions[s][0] = random.nextDouble(-10_000.0, 10_000.0);
            streamPositions[s][1] = random.nextDouble(-10_000.0, 10_000.0);
            streamPositions[s][2] = random.nextDouble(-10_000.0, 10_000.0);
        }

        for (int i = 0; i < count; i++) {
            int streamId = i % 3;
            double[] pos = streamPositions[streamId];

            // Pick random offsets from the 100-element pool
            pos[0] += offsetPool[random.nextInt(100)];
            pos[1] += offsetPool[random.nextInt(100)];
            pos[2] += offsetPool[random.nextInt(100)];

            packets.add(new Packet(streamId, pos[0], pos[1], pos[2]));
        }

        return packets;
    }

    private static Result benchmarkLZ4(
            Path file,
            List<Packet> packets
    ) throws IOException {

        long start = System.nanoTime();

        try (LZ4PositionWriter writer = new LZ4PositionWriter(file)) {
            for (Packet p : packets) {
                writer.writePacket(
                        p.streamId(),
                        p.x(),
                        p.y(),
                        p.z()
                );
            }

            writer.flush();
        }

        long elapsed = System.nanoTime() - start;

        return new Result(
                "LINEFOLD",
                Files.size(file),
                elapsed
        );
    }

    private static Result benchmarkJSON(
            Path file,
            List<Packet> packets
    ) throws IOException {

        long start = System.nanoTime();

        try (BufferedWriter out = Files.newBufferedWriter(
                file,
                StandardCharsets.UTF_8
        )) {
            for (Packet p : packets) {
                out.write("{\"streamId\":");
                out.write(Integer.toString(p.streamId()));

                out.write(",\"x\":");
                out.write(Double.toString(p.x()));

                out.write(",\"y\":");
                out.write(Double.toString(p.y()));

                out.write(",\"z\":");
                out.write(Double.toString(p.z()));

                out.write("}\n");
            }
        }

        long elapsed = System.nanoTime() - start;

        return new Result(
                "JSONL",
                Files.size(file),
                elapsed
        );
    }

    private static Result benchmarkBinary(
            Path file,
            List<Packet> packets
    ) throws IOException {

        long start = System.nanoTime();

        try (OutputStream out = new BufferedOutputStream(
                Files.newOutputStream(file)
        )) {
            ByteBuffer buffer = ByteBuffer.allocate(28)
                    .order(ByteOrder.BIG_ENDIAN);

            for (Packet p : packets) {
                buffer.clear();

                buffer.putInt(p.streamId());
                buffer.putDouble(p.x());
                buffer.putDouble(p.y());
                buffer.putDouble(p.z());

                out.write(buffer.array());
            }
        }

        long elapsed = System.nanoTime() - start;

        return new Result(
                "Raw binary",
                Files.size(file),
                elapsed
        );
    }
}