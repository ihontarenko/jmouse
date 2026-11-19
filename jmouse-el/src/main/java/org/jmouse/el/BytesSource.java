package org.jmouse.el;

import org.jmouse.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

/** Storage-independent read-only bytes. Only array sources imply an owned immutable snapshot. */
public interface BytesSource extends Source, AutoCloseable {
    static BytesSource copyOf(String name, byte[] bytes) {
        return ArrayBytesSource.copyOf(name, bytes);
    }

    /** Reads an owned bounded snapshot while leaving the supplied stream open. */
    static BytesSource read(String name, InputStream input, BytesSourceOptions options)
            throws IOException {
        return BytesSourceSnapshots.read(name, input, options);
    }

    static BytesSource open(Resource resource, BytesSourceOptions options) throws IOException {
        if (resource.isFile()) {
            return open(resource.getFile().toPath(), options);
        }
        try (InputStream input = resource.getInputStream()) {
            return read(resource.getName(), input, options);
        }
    }

    static BytesSource open(Path path) throws IOException {
        return open(path, BytesSourceOptions.standard());
    }

    static BytesSource open(Path path, BytesSourceOptions options) throws IOException {
        return FileBytesSource.open(path, options);
    }

    @Override
    default InputUnit unit() {
        return InputUnit.BYTE;
    }

    int unsigned8(long offset);

    /** A deliberate owned snapshot; a file view never silently claims snapshot immutability. */
    default BytesSource snapshot(BytesSourceOptions options) throws IOException {
        if (contentLength() > options.maximumSnapshotBytes()) {
            throw new IOException(
                    "Byte source exceeds the configured snapshot limit: " + getName());
        }
        return ArrayBytesSource.owned(getName(), slice(0, Math.toIntExact(contentLength())));
    }

    default void read(long offset, byte[] target, int targetOffset, int length) {
        Objects.checkFromIndexSize(offset, length, contentLength());
        Objects.checkFromIndexSize(targetOffset, length, target.length);
        for (int index = 0; index < length; index++) {
            target[targetOffset + index] = (byte) unsigned8(offset + index);
        }
    }

    default byte[] slice(long offset, int length) {
        Objects.checkFromIndexSize(offset, length, contentLength());
        byte[] bytes = new byte[length];
        read(offset, bytes, 0, length);
        return bytes;
    }

    default String digest() {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[BytesSourceOptions.standard().bufferSize()];
            long offset = 0;
            while (offset < contentLength()) {
                int length = (int) Math.min(buffer.length, contentLength() - offset);
                read(offset, buffer, 0, length);
                digest.update(buffer, 0, length);
                offset += length;
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required by the Java platform", exception);
        }
    }

    @Override
    default void close() throws IOException {}
}
