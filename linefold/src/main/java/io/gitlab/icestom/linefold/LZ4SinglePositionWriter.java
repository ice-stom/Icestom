package io.gitlab.icestom.linefold;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;

public final class LZ4SinglePositionWriter extends AbstractLZ4PositionWriter {

    private static final Logger log = LoggerFactory.getLogger(LZ4SinglePositionWriter.class);

    private static final int RECORD_BYTES = 24;

    private long prevX = 0L;
    private long prevY = 0L;
    private long prevZ = 0L;

    public LZ4SinglePositionWriter(Path path) throws IOException {
        this(path, DEFAULT_BUFFER_CAPACITY_BYTES);
    }

    public LZ4SinglePositionWriter(Path path, int bufferCapacityBytes) throws IOException {
        super(path, bufferCapacityBytes, RECORD_BYTES, log);
    }

    public void writePacket(double x, double y, double z) {
        if (closed) {
            throw new IllegalStateException("writer is closed");
        }

        long v1 = Double.doubleToRawLongBits(x);
        long v2 = Double.doubleToRawLongBits(y);
        long v3 = Double.doubleToRawLongBits(z);

        long d1 = v1 ^ prevX;
        long d2 = v2 ^ prevY;
        long d3 = v3 ^ prevZ;
        prevX = v1;
        prevY = v2;
        prevZ = v3;

        ensureCapacity();
        active.putLong(d1);
        active.putLong(d2);
        active.putLong(d3);
    }

    @Override
    public void flush() {
        doFlush();
    }
}
