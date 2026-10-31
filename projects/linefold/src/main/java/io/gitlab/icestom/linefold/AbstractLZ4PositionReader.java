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

    protected abstract void processRecord(byte[] decompressed, int[] offsetHolder);

    protected final void readAllFrames(Path path) throws IOException {
        LZ4FastDecompressor decompressor = LZ4Factory.fastestInstance().fastDecompressor();

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

                int[] offsetHolder = new int[1];
                offsetHolder[0] = 0;
                while (offsetHolder[0] < decompressed.length) {
                    processRecord(decompressed, offsetHolder);
                }
            }
        }
    }

    protected static long readVarint(byte[] b, int[] offsetHolder) {
        int off = offsetHolder[0];
        long result = 0;
        int shift = 0;
        byte bVal;
        do {
            bVal = b[off++];
            result |= (long) (bVal & 0x7F) << shift;
            shift += 7;
        } while ((bVal & 0x80) != 0);
        offsetHolder[0] = off;
        return result;
    }

    protected static int readVarintInt(byte[] b, int[] offsetHolder) {
        int off = offsetHolder[0];
        int result = 0;
        int shift = 0;
        byte bVal;
        do {
            bVal = b[off++];
            result |= (bVal & 0x7F) << shift;
            shift += 7;
        } while ((bVal & 0x80) != 0);
        offsetHolder[0] = off;
        return result;
    }
}