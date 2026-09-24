package io.gitlab.icestom.linefold;

import net.jpountz.lz4.LZ4Factory;
import net.jpountz.lz4.LZ4FastDecompressor;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

abstract class AbstractLZ4PositionReader {

    protected AbstractLZ4PositionReader() {}

    protected abstract int recordBytes();

    protected abstract void processRecord(byte[] decompressed, int offset);

    protected final void readAllFrames(Path path) throws IOException {
        LZ4FastDecompressor decompressor = LZ4Factory.fastestInstance().fastDecompressor();
        final int recordBytes = recordBytes();

        try (DataInputStream in = new DataInputStream(
                new BufferedInputStream(Files.newInputStream(path)))) {

            while (true) {
                int uncompressedLength;
                try {
                    uncompressedLength = in.readInt();
                } catch (EOFException eof) {
                    break;
                }
                int compressedLength = in.readInt();

                byte[] compressed = new byte[compressedLength];
                in.readFully(compressed);

                byte[] decompressed = new byte[uncompressedLength];
                decompressor.decompress(compressed, 0, decompressed, 0, uncompressedLength);

                if (decompressed.length % recordBytes != 0) {
                    throw new IOException("Frame length " + decompressed.length
                            + " is not a multiple of the record size (" + recordBytes + ")");
                }

                for (int offset = 0; offset < decompressed.length; offset += recordBytes) {
                    processRecord(decompressed, offset);
                }
            }
        }
    }

    protected static int readInt(byte[] b, int off) {
        return ((b[off] & 0xFF) << 24)
                | ((b[off + 1] & 0xFF) << 16)
                | ((b[off + 2] & 0xFF) << 8)
                | (b[off + 3] & 0xFF);
    }

    protected static long readLong(byte[] b, int off) {
        long v = 0L;
        for (int i = 0; i < 8; i++) {
            v = (v << 8) | (b[off + i] & 0xFF);
        }
        return v;
    }
}
