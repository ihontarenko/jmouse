package org.jmouse.storage.support;

import org.jmouse.storage.Content;
import org.jmouse.storage.exception.StorageException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * 🧮 Copies content somewhere while measuring and digesting it in the same pass.
 *
 * <p>Shared by every backend rather than reimplemented per backend: what a write has to establish
 * about the bytes — how many arrived, and what they hash to — does not depend on where they land.
 * A local backend copies into the destination directory before moving the file into place; an
 * object store spools to a temporary file because a put must declare its length up front and a
 * stream cannot answer that. Both are this method with a different target.</p>
 */
public final class ContentDigests {

    /**
     * 🔐 The digest every backend records, chosen once so an object's identity does not depend on
     * which backend happened to write it.
     */
    public static final String ALGORITHM = "SHA-256";

    private ContentDigests() {
    }

    /**
     * 📥 Copy content into a file, digesting as it goes.
     *
     * @param content content to copy
     * @param target  file to write
     * @return the size and digest of what actually arrived
     * @throws IOException on read or write failure
     */
    public static Digested copyTo(Content content, Path target) throws IOException {
        MessageDigest digest = newDigest();
        long          sizeBytes;

        try (InputStream source = content.stream().open();
             DigestInputStream digesting = new DigestInputStream(source, digest);
             OutputStream sink = Files.newOutputStream(target)) {
            sizeBytes = digesting.transferTo(sink);
        }

        return new Digested(sizeBytes, HexFormat.of().formatHex(digest.digest()));
    }

    /**
     * 🔐 A fresh digest.
     *
     * @return the digest to accumulate into
     * @throws StorageException when the algorithm is unavailable, which no supported runtime allows
     */
    private static MessageDigest newDigest() {
        try {
            return MessageDigest.getInstance(ALGORITHM);
        } catch (NoSuchAlgorithmException exception) {
            throw new StorageException("Algorithm '%s' is unavailable".formatted(ALGORITHM), exception);
        }
    }
}
