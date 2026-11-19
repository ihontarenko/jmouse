package org.jmouse.el;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

/** An owned immutable snapshot, with a cached content digest and bulk-copy access. */
final class ArrayBytesSource implements BytesSource {
    private final String name;
    private final byte[] bytes;
    private final String digest;

    private ArrayBytesSource(String name, byte[] bytes) {
        this.name = Objects.requireNonNull(name, "name");
        this.bytes = bytes;
        try {
            digest =
                    HexFormat.of()
                            .formatHex(MessageDigest.getInstance("SHA-256").digest(this.bytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required by the Java platform", exception);
        }
    }

    static BytesSource copyOf(String name, byte[] bytes) {
        return new ArrayBytesSource(name, bytes.clone());
    }

    static BytesSource owned(String name, byte[] bytes) {
        return new ArrayBytesSource(name, bytes);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public long contentLength() {
        return bytes.length;
    }

    @Override
    public String digest() {
        return digest;
    }

    @Override
    public BytesSource snapshot(BytesSourceOptions options) throws IOException {
        if (bytes.length > options.maximumSnapshotBytes()) {
            throw new IOException("Byte source exceeds the configured snapshot limit: " + name);
        }
        return this;
    }

    @Override
    public int unsigned8(long offset) {
        Objects.checkIndex(offset, bytes.length);
        return Byte.toUnsignedInt(bytes[(int) offset]);
    }

    @Override
    public void read(long offset, byte[] target, int targetOffset, int length) {
        Objects.checkFromIndexSize(offset, length, bytes.length);
        Objects.checkFromIndexSize(targetOffset, length, target.length);
        System.arraycopy(bytes, (int) offset, target, targetOffset, length);
    }
}
