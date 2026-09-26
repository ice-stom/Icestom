package io.gitlab.icestom.linefold;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class LZ4PositionWriter extends AbstractLZ4PositionWriter {

    private static final Logger log = LoggerFactory.getLogger(LZ4PositionWriter.class);

    private static final int RECORD_BYTES = 28;

    private final Map<Integer, long[]> prevByStream = new ConcurrentHashMap<>();
    private final Object bufferLock = new Object();

    public LZ4PositionWriter(Path path) throws IOException {
        this(path, DEFAULT_BUFFER_CAPACITY_BYTES);
    }

    public LZ4PositionWriter(Path path, int bufferCapacityBytes) throws IOException {
        super(path, bufferCapacityBytes, RECORD_BYTES, log);
    }

    public void writePacket(int streamId, double x, double y, double z) {
        if (closed) {
            throw new IllegalStateException("writer is closed");
        }

        long v1 = Double.doubleToRawLongBits(x);
        long v2 = Double.doubleToRawLongBits(y);
        long v3 = Double.doubleToRawLongBits(z);

        long[] prev = prevByStream.computeIfAbsent(streamId, k -> new long[3]);
        long d1, d2, d3;
        synchronized (prev) {
            d1 = v1 ^ prev[0];
            d2 = v2 ^ prev[1];
            d3 = v3 ^ prev[2];
            prev[0] = v1;
            prev[1] = v2;
            prev[2] = v3;
        }

        synchronized (bufferLock) {
            ensureCapacity();
            active.putInt(streamId);
            active.putLong(d1);
            active.putLong(d2);
            active.putLong(d3);
        }
    }

    @Override
    public void flush() {
        synchronized (bufferLock) {
            doFlush();
        }
    }

    @Override
    protected void returnSpare(ByteBuffer buffer) {
        synchronized (bufferLock) {
            super.returnSpare(buffer);
        }
    }
}
