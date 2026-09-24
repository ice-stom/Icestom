package io.gitlab.icestom.linefold;

import net.jpountz.lz4.LZ4Compressor;
import net.jpountz.lz4.LZ4Factory;
import org.slf4j.Logger;

import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

abstract class AbstractLZ4PositionWriter implements Closeable {

    protected static final int DEFAULT_BUFFER_CAPACITY_BYTES = 1 << 20;

    private final int recordBytes;
    private final LZ4Compressor compressor = LZ4Factory.fastestInstance().fastCompressor();
    private final DataOutputStream fileOut;
    private final Object fileLock = new Object();
    private final Logger log;

    private final ExecutorService compressionExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "lz4-position-writer");
        t.setDaemon(true);
        return t;
    });

    private final BlockingQueue<ByteBuffer> pendingFlushSlots = new ArrayBlockingQueue<>(2);

    protected ByteBuffer active;
    protected ByteBuffer spare;
    protected volatile boolean closed = false;

    protected AbstractLZ4PositionWriter(Path path, int bufferCapacityBytes, int recordBytes, Logger log)
            throws IOException {
        if (bufferCapacityBytes < recordBytes) {
            throw new IllegalArgumentException("bufferCapacityBytes must hold at least one record");
        }
        this.recordBytes = recordBytes;
        this.log = log;
        this.fileOut = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(path)));
        this.active = ByteBuffer.allocate(bufferCapacityBytes);
        this.spare = ByteBuffer.allocate(bufferCapacityBytes);
    }

    protected final void ensureCapacity() {
        if (active.remaining() < recordBytes) {
            swapAndScheduleFlush();
        }
    }

    protected final void doFlush() {
        if (active.position() > 0) {
            swapAndScheduleFlush();
        }
    }

    public abstract void flush();

    private void swapAndScheduleFlush() {
        final ByteBuffer full = active;
        active = spare;
        active.clear();
        spare = null;

        full.flip();
        try {
            pendingFlushSlots.put(full);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("interrupted while queuing buffer for compression", e);
        }

        compressionExecutor.submit(() -> compressAndWrite(full));
    }

    private void compressAndWrite(ByteBuffer readyBuffer) {
        try {
            int uncompressedLength = readyBuffer.remaining();
            byte[] src = new byte[uncompressedLength];
            readyBuffer.get(src);

            int maxCompressedLength = compressor.maxCompressedLength(uncompressedLength);
            byte[] dst = new byte[maxCompressedLength];
            int compressedLength = compressor.compress(src, 0, uncompressedLength, dst, 0, maxCompressedLength);

            synchronized (fileLock) {
                fileOut.writeInt(uncompressedLength);
                fileOut.writeInt(compressedLength);
                fileOut.write(dst, 0, compressedLength);
            }
        } catch (IOException e) {
            log.error("failed to write compressed position frame", e);
        } finally {
            pendingFlushSlots.remove(readyBuffer);
            readyBuffer.clear();
            returnSpare(readyBuffer);
        }
    }

    protected void returnSpare(ByteBuffer buffer) {
        if (spare == null) {
            spare = buffer;
        }
    }

    @Override
    public void close() throws IOException {
        if (closed) {
            return;
        }
        closed = true;
        flush();
        compressionExecutor.shutdown();
        try {
            compressionExecutor.awaitTermination(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        synchronized (fileLock) {
            fileOut.flush();
            fileOut.close();
        }
    }
}
