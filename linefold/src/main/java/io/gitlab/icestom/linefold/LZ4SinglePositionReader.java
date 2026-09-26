package io.gitlab.icestom.linefold;

import java.io.IOException;
import java.nio.file.Path;

public final class LZ4SinglePositionReader extends AbstractLZ4PositionReader {

    private static final int RECORD_BYTES = 24;

    private final PacketHandler handler;
    private long prevX = 0L;
    private long prevY = 0L;
    private long prevZ = 0L;

    private LZ4SinglePositionReader(PacketHandler handler) {
        this.handler = handler;
    }

    @FunctionalInterface
    public interface PacketHandler {
        void onPacket(double x, double y, double z);
    }

    public static void readAll(Path path, PacketHandler handler) throws IOException {
        new LZ4SinglePositionReader(handler).readAllFrames(path);
    }

    @Override
    protected int recordBytes() {
        return RECORD_BYTES;
    }

    @Override
    protected void processRecord(byte[] decompressed, int offset) {
        long d1 = readLong(decompressed, offset);
        long d2 = readLong(decompressed, offset + 8);
        long d3 = readLong(decompressed, offset + 16);

        long v1 = d1 ^ prevX;
        long v2 = d2 ^ prevY;
        long v3 = d3 ^ prevZ;
        prevX = v1;
        prevY = v2;
        prevZ = v3;

        handler.onPacket(
                Double.longBitsToDouble(v1),
                Double.longBitsToDouble(v2),
                Double.longBitsToDouble(v3));
    }
}
