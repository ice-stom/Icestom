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
    protected static final int MAX_RECORD_BYTES = 40;

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
    private final BlockingQueue<ByteBuffer> bufferPool = new ArrayBlockingQueue<>(2);

    protected ByteBuffer active;
    protected volatile boolean closed = false;

    protected AbstractLZ4PositionWriter(Path path, int bufferCapacityBytes, Logger log)
            throws IOException {
        if (bufferCapacityBytes < MAX_RECORD_BYTES) {
            throw new IllegalArgumentException("bufferCapacityBytes must hold at least one record");
        }
        this.log = log;
        this.fileOut = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(path)));

        ByteBuffer buf1 = ByteBuffer.allocate(bufferCapacityBytes);
        ByteBuffer buf2 = ByteBuffer.allocate(bufferCapacityBytes);
        try {
            bufferPool.put(buf1);
            bufferPool.put(buf2);
            this.active = bufferPool.take();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("interrupted while initializing buffers", e);
        }
    }

    protected final void ensureCapacity() {
        if (active.remaining() < MAX_RECORD_BYTES) {
            swapAndScheduleFlush();
        }
    }

    protected final void doFlush() {
        if (active.position() > 0) {
            swapAndScheduleFlush();
        }
    }

    public abstract void flush();

    protected static void putVarint(ByteBuffer buf, long v) {
        while ((v & ~0x7FL) != 0L) {
            buf.put((byte) ((v & 0x7F) | 0x80));
            v >>>= 7;
        }
        buf.put((byte) (v & 0x7F));
    }

    protected static void putVarintInt(ByteBuffer buf, int v) {
        while ((v & ~0x7F) != 0) {
            buf.put((byte) ((v & 0x7F) | 0x80));
            v >>>= 7;
        }
        buf.put((byte) (v & 0x7F));
    }

    private void swapAndScheduleFlush() {
        final ByteBuffer full = active;
        try {
            active = bufferPool.take();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("interrupted while waiting for buffer", e);
        }
        active.clear();

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
            try {
                bufferPool.put(readyBuffer);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
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