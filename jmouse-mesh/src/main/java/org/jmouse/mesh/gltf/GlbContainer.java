package org.jmouse.mesh.gltf;

import org.jmouse.mesh.MeshReadException;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Optional;

/**
 * The binary glTF container (glTF 2.0, section 4.4): a twelve-byte header, then a JSON chunk saying what is where and
 * an optional binary chunk holding it, each padded to four bytes. Chunks of another type are skipped, as the standard
 * asks.
 */
final class GlbContainer {

    private static final int MAGIC = 0x46546C67;
    private static final int VERSION = 2;
    private static final int JSON_CHUNK = 0x4E4F534A;
    private static final int BINARY_CHUNK = 0x004E4942;
    private static final int HEADER = 12;
    private static final int CHUNK_HEADER = 8;

    /** What a GLB file holds: its JSON, and the bytes of its first buffer when it carries them. */
    record Chunks(byte[] json, Optional<ByteBuffer> binary) {
    }

    private GlbContainer() {
    }

    static byte[] write(byte[] json, byte[] binary) {
        byte[] paddedJson = padded(json, (byte) ' ');
        byte[] paddedBinary = padded(binary, (byte) 0);
        int length = HEADER + CHUNK_HEADER + paddedJson.length + CHUNK_HEADER + paddedBinary.length;
        ByteBuffer buffer = ByteBuffer.allocate(length).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(MAGIC).putInt(VERSION).putInt(length);
        buffer.putInt(paddedJson.length).putInt(JSON_CHUNK).put(paddedJson);
        buffer.putInt(paddedBinary.length).putInt(BINARY_CHUNK).put(paddedBinary);
        return buffer.array();
    }

    /** @throws MeshReadException when the bytes are not a GLB file of version 2 */
    static Chunks read(byte[] bytes, String name) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        if (bytes.length < HEADER + CHUNK_HEADER || buffer.getInt(0) != MAGIC) {
            throw new MeshReadException(name + ": not binary glTF — it does not open with 'glTF'");
        }
        if (buffer.getInt(4) != VERSION) {
            throw new MeshReadException(name + ": binary glTF of version " + buffer.getInt(4) + "; only version 2 is read");
        }
        int length = (int) Math.min(Integer.toUnsignedLong(buffer.getInt(8)), bytes.length);
        byte[] json = null;
        ByteBuffer binary = null;
        int at = HEADER;
        while (at + CHUNK_HEADER <= length) {
            int chunkLength = buffer.getInt(at);
            int type = buffer.getInt(at + 4);
            int start = at + CHUNK_HEADER;
            if (chunkLength < 0 || start + chunkLength > length) {
                throw new MeshReadException(name + ": a chunk at byte " + at + " runs past the end of the file");
            }
            if (type == JSON_CHUNK && json == null) {
                json = Arrays.copyOfRange(bytes, start, start + chunkLength);
            } else if (type == BINARY_CHUNK && binary == null) {
                binary = ByteBuffer.wrap(bytes, start, chunkLength).slice().order(ByteOrder.LITTLE_ENDIAN);
            }
            at = start + chunkLength;
        }
        if (json == null) {
            throw new MeshReadException(name + ": binary glTF with no JSON chunk");
        }
        return new Chunks(json, Optional.ofNullable(binary));
    }

    /** A chunk is padded to four bytes: JSON with spaces, binary with zeros. */
    private static byte[] padded(byte[] bytes, byte filler) {
        int length = (bytes.length + 3) & ~3;
        byte[] padded = Arrays.copyOf(bytes, length);
        Arrays.fill(padded, bytes.length, length, filler);
        return padded;
    }
}
