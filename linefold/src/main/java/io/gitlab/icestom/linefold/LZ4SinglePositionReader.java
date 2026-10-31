package io.gitlab.icestom.linefold;

import java.io.IOException;
import java.nio.file.Path;

public final class LZ4SinglePositionReader extends AbstractLZ4PositionReader {

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
    protected void processRecord(byte[] decompressed, int[] offsetHolder) {
        long d1 = readVarint(decompressed, offsetHolder);
        long d2 = readVarint(decompressed, offsetHolder);
        long d3 = readVarint(decompressed, offsetHolder);

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