package io.gitlab.icestom.linefold;

import java.io.OutputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class LZ4PositionWriter extends AbstractLZ4PositionWriter {

    private final Map<Integer, long[]> prevByStream = new ConcurrentHashMap<>();
    private final Object bufferLock = new Object();

    public LZ4PositionWriter(OutputStream out) {
        this(out, DEFAULT_BUFFER_CAPACITY_BYTES);
    }

    public LZ4PositionWriter(OutputStream out, int bufferCapacityBytes) {
        super(out, bufferCapacityBytes);
    }

    public void writePacket(int streamId, double x, double y, double z, float yaw) {
        if (closed) {
            throw new IllegalStateException("writer is closed");
        }

        long v1 = Double.doubleToRawLongBits(x);
        long v2 = Double.doubleToRawLongBits(y);
        long v3 = Double.doubleToRawLongBits(z);
        long v4 = Float.floatToRawIntBits(yaw) & 0xFFFFFFFFL;

        long[] prev = prevByStream.computeIfAbsent(streamId, k -> new long[4]);
        long d1, d2, d3, d4;

        synchronized (prev) {
            d1 = v1 ^ prev[0];
            d2 = v2 ^ prev[1];
            d3 = v3 ^ prev[2];
            d4 = v4 ^ prev[3];
            prev[0] = v1;
            prev[1] = v2;
            prev[2] = v3;
            prev[3] = v4;
        }

        synchronized (bufferLock) {
            ensureCapacity();
            putVarintInt(active, streamId);
            putVarint(active, d1);
            putVarint(active, d2);
            putVarint(active, d3);
            putVarint(active, d4);
        }
    }

    @Override
    public void flush() {
        synchronized (bufferLock) {
            doFlush();
        }
    }
}