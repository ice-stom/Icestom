package io.gitlab.icestom.linefold;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public final class LZ4PositionReader extends AbstractLZ4PositionReader {

    private final PacketHandler handler;
    private final Map<Integer, long[]> prevByStream = new HashMap<>();

    private LZ4PositionReader(PacketHandler handler) {
        this.handler = handler;
    }

    @FunctionalInterface
    public interface PacketHandler {
        void onPacket(int streamId, double x, double y, double z);
    }

    public static void readAll(Path path, PacketHandler handler) throws IOException {
        new LZ4PositionReader(handler).readAllFrames(path);
    }

    @Override
    protected void processRecord(byte[] decompressed, int[] offsetHolder) {
        int streamId = readVarintInt(decompressed, offsetHolder);
        long d1 = readVarint(decompressed, offsetHolder);
        long d2 = readVarint(decompressed, offsetHolder);
        long d3 = readVarint(decompressed, offsetHolder);

        long[] prev = prevByStream.computeIfAbsent(streamId, k -> new long[3]);
        long v1 = d1 ^ prev[0];
        long v2 = d2 ^ prev[1];
        long v3 = d3 ^ prev[2];
        prev[0] = v1;
        prev[1] = v2;
        prev[2] = v3;

        handler.onPacket(
                streamId,
                Double.longBitsToDouble(v1),
                Double.longBitsToDouble(v2),
                Double.longBitsToDouble(v3));
    }
}