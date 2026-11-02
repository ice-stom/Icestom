package io.gitlab.icestom.linefold;

import java.io.IOException;
import java.io.InputStream;

public final class LZ4SinglePositionReader extends AbstractLZ4PositionReader {

    private final PacketHandler handler;
    private long prevX = 0L;
    private long prevY = 0L;
    private long prevZ = 0L;
    private long prevYaw = 0L;

    private LZ4SinglePositionReader(PacketHandler handler) {
        this.handler = handler;
    }

    @FunctionalInterface
    public interface PacketHandler {
        void onPacket(double x, double y, double z, float yaw);
    }

    public static void readAll(InputStream in, PacketHandler handler) throws IOException {
        new LZ4SinglePositionReader(handler).readAllFrames(in);
    }

    @Override
    protected void processRecord(byte[] decompressed, int[] offsetHolder) {
        long d1 = readVarint(decompressed, offsetHolder);
        long d2 = readVarint(decompressed, offsetHolder);
        long d3 = readVarint(decompressed, offsetHolder);
        long d4 = readVarint(decompressed, offsetHolder);

        long v1 = d1 ^ prevX;
        long v2 = d2 ^ prevY;
        long v3 = d3 ^ prevZ;
        long v4 = d4 ^ prevYaw;
        prevX = v1;
        prevY = v2;
        prevZ = v3;
        prevYaw = v4;

        handler.onPacket(
                Double.longBitsToDouble(v1),
                Double.longBitsToDouble(v2),
                Double.longBitsToDouble(v3),
                Float.intBitsToFloat((int) v4));
    }
}