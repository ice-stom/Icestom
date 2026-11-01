package io.gitlab.icestom.linefold;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.OutputStream;

public final class LZ4SinglePositionWriter extends AbstractLZ4PositionWriter {

    private static final Logger log = LoggerFactory.getLogger(LZ4SinglePositionWriter.class);

    private long prevX = 0L;
    private long prevY = 0L;
    private long prevZ = 0L;
    private long prevYaw = 0L;

    public LZ4SinglePositionWriter(OutputStream out) {
        this(out, DEFAULT_BUFFER_CAPACITY_BYTES);
    }

    public LZ4SinglePositionWriter(OutputStream out, int bufferCapacityBytes) {
        super(out, bufferCapacityBytes, log);
    }

    public void writePacket(double x, double y, double z, float yaw) {
        if (closed) {
            throw new IllegalStateException("writer is closed");
        }

        long v1 = Double.doubleToRawLongBits(x);
        long v2 = Double.doubleToRawLongBits(y);
        long v3 = Double.doubleToRawLongBits(z);
        long v4 = Float.floatToRawIntBits(yaw) & 0xFFFFFFFFL;

        long d1 = v1 ^ prevX;
        long d2 = v2 ^ prevY;
        long d3 = v3 ^ prevZ;
        long d4 = v4 ^ prevYaw;
        prevX = v1;
        prevY = v2;
        prevZ = v3;
        prevYaw = v4;

        ensureCapacity();
        putVarint(active, d1);
        putVarint(active, d2);
        putVarint(active, d3);
        putVarint(active, d4);
    }

    @Override
    public void flush() {
        doFlush();
    }
}