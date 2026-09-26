package io.gitlab.icestom.linefold;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public final class LZ4PositionReader extends AbstractLZ4PositionReader {

    private static final int RECORD_BYTES = 28;

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
    protected int recordBytes() {
        return RECORD_BYTES;
    }

    @Override
    protected void processRecord(byte[] decompressed, int offset) {
        int streamId = readInt(decompressed, offset);
        long d1 = readLong(decompressed, offset + 4);
        long d2 = readLong(decompressed, offset + 12);
        long d3 = readLong(decompressed, offset + 20);

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
