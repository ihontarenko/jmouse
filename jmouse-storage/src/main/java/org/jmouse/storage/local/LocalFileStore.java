package org.jmouse.storage.local;

import org.jmouse.core.MediaType;
import org.jmouse.core.io.FileSystemResource;
import org.jmouse.core.io.Resource;
import org.jmouse.core.io.ResourceSegment;
import org.jmouse.http.Range;
import org.jmouse.storage.Content;
import org.jmouse.storage.ContentTypes;
import org.jmouse.storage.FileStore;
import org.jmouse.storage.ObjectDescription;
import org.jmouse.storage.StorageKey;
import org.jmouse.storage.StoredObject;
import org.jmouse.storage.configuration.StorageSettings;
import org.jmouse.storage.exception.ObjectNotFoundException;
import org.jmouse.storage.exception.StorageException;
import org.jmouse.storage.exception.StorageKeyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * 💾 The reference {@link FileStore}: objects on local disk, the storage key being their path
 * relative to a configured root.
 *
 * <p>Deliberately does not implement {@code resolveDirectLink}. The disk is reachable only through
 * this application, so every download is streamed — which is exactly the case every caller has to
 * handle anyway, since a direct link is optional by contract.</p>
 *
 * <p>Writes land on a temporary file in the destination directory and are then moved into place, so
 * a failed write cannot leave a truncated object behind at a key that already reads as valid.</p>
 */
public class LocalFileStore implements FileStore {

    /**
     * 🏷️ Name recorded against every object this backend writes.
     */
    public static final String BACKEND_NAME = "local";

    private static final Logger LOGGER           = LoggerFactory.getLogger(LocalFileStore.class);
    private static final String DIGEST_ALGORITHM = "SHA-256";
    private static final String TEMPORARY_PREFIX = ".jmouse-storage-";
    private static final String TEMPORARY_SUFFIX = ".part";

    private final Path root;

    /**
     * 🏗️ Store objects under the configured storage directory.
     *
     * @param settings the active storage settings
     */
    public LocalFileStore(StorageSettings settings) {
        this(Path.of(settings.storageDirectory()));
    }

    /**
     * 🏗️ Store objects under an explicit root.
     *
     * @param root directory every key resolves against
     */
    public LocalFileStore(Path root) {
        this.root = root.toAbsolutePath().normalize();
        LOGGER.info("Storage backend '{}' rooted at {}", BACKEND_NAME, this.root);
    }

    @Override
    public String backendName() {
        return BACKEND_NAME;
    }

    @Override
    public StoredObject write(StorageKey key, Content content) {
        Path target    = resolve(key);
        Path directory = target.getParent();
        Path temporary = null;

        try {
            Files.createDirectories(directory);
            temporary = Files.createTempFile(directory, TEMPORARY_PREFIX, TEMPORARY_SUFFIX);

            Digested digested = copyDigesting(content, temporary);

            // REPLACE_EXISTING without ATOMIC_MOVE: the two together are rejected on Windows, and
            // a same-directory move is already atomic enough that no reader sees a partial object.
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            temporary = null;

            return new StoredObject(key, digested.sizeBytes(), ContentTypes.resolve(content, key),
                                    digested.sha256());
        } catch (IOException exception) {
            throw new StorageException("Failed to write '%s': %s".formatted(key, exception.getMessage()), exception);
        } finally {
            discard(temporary);
        }
    }

    @Override
    public Resource read(StorageKey key) {
        return new FileSystemResource(requireExisting(key));
    }

    @Override
    public ResourceSegment readRange(StorageKey key, Range range) {
        Resource resource = read(key);
        long     length   = resource.getLength();
        long     start    = range.getStart(length);
        long     end      = Math.min(range.getEnd(length), length - 1);

        if (start < 0 || start >= length || end < start) {
            throw new StorageException(
                    "Range %s cannot be satisfied by '%s' of %d bytes".formatted(range.toHeaderValue(), key, length));
        }

        return ResourceSegment.ofRange(start, resource, end - start + 1);
    }

    @Override
    public ObjectDescription describe(StorageKey key) {
        Path path = requireExisting(key);

        try {
            return new ObjectDescription(key, Files.size(path), describeContentType(key, path));
        } catch (IOException exception) {
            throw new StorageException("Failed to inspect '%s': %s".formatted(key, exception.getMessage()), exception);
        }
    }

    /**
     * 🎨 Establish the content type of a stored object without reading it.
     *
     * <p>The key's extension answers first, since it is the same source {@code write} preferred
     * once the caller's declaration ran out. A key composed without an extension leaves nothing to
     * go on, so the file system is asked before falling back — which is what keeps
     * {@code describe} from contradicting the receipt {@code write} handed back.</p>
     *
     * @param key  key of the stored object
     * @param path where it actually lives
     * @return a content type, never {@code null}
     * @throws IOException when probing the file system fails
     */
    private MediaType describeContentType(StorageKey key, Path path) throws IOException {
        MediaType byExtension = ContentTypes.forFilename(key.value());

        if (byExtension != null) {
            return byExtension;
        }

        MediaType probed = ContentTypes.parse(Files.probeContentType(path));

        return (probed != null) ? probed : ContentTypes.DEFAULT;
    }

    /**
     * 🗑️ Remove the object, reporting failure only to the log.
     *
     * <p>Nothing here escapes — not a missing object, not an I/O error, not even a key that
     * resolves outside the root. Deletion is what a caller runs while cleaning up after something
     * else went wrong, and a cleanup that can itself throw turns one failure into two.</p>
     */
    @Override
    public void delete(StorageKey key) {
        try {
            if (!Files.deleteIfExists(resolve(key))) {
                LOGGER.debug("Nothing to delete under storage key '{}'", key);
            }
        } catch (IOException | RuntimeException exception) {
            LOGGER.warn("Could not delete '{}' from storage", key, exception);
        }
    }

    /**
     * 📥 Copy content into a file while digesting it, so identity costs no second read.
     *
     * @param content content to copy
     * @param target  file to write
     * @return the size and digest of what actually arrived
     * @throws IOException on read or write failure
     */
    private Digested copyDigesting(Content content, Path target) throws IOException {
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
     * 📂 Resolve a key against the root, refusing anything that lands outside it.
     *
     * <p>{@link StorageKey} has already rejected every malformed shape; this re-checks the
     * <em>resolved</em> path, because symlinks and platform quirks can still walk out of a root
     * that a purely textual check would have approved.</p>
     *
     * @param key key to resolve
     * @return the absolute path of the object
     */
    private Path resolve(StorageKey key) {
        Path resolved = root.resolve(key.value()).normalize();

        if (!resolved.startsWith(root)) {
            throw new StorageKeyException("Storage key '%s' resolves outside the storage root".formatted(key));
        }

        return resolved;
    }

    /**
     * 🔍 Resolve a key that must already hold an object.
     *
     * @param key key to resolve
     * @return the absolute path of the object
     * @throws ObjectNotFoundException when nothing is stored there
     */
    private Path requireExisting(StorageKey key) {
        Path path = resolve(key);

        if (!Files.isRegularFile(path)) {
            throw new ObjectNotFoundException(key);
        }

        return path;
    }

    /**
     * 🧹 Remove a temporary file left over from a failed write.
     *
     * @param temporary the file to remove, or {@code null} when the write succeeded
     */
    private void discard(Path temporary) {
        if (temporary == null) {
            return;
        }

        try {
            Files.deleteIfExists(temporary);
        } catch (IOException exception) {
            LOGGER.warn("Could not discard partial write at {}", temporary, exception);
        }
    }

    private static MessageDigest newDigest() {
        try {
            return MessageDigest.getInstance(DIGEST_ALGORITHM);
        } catch (NoSuchAlgorithmException exception) {
            throw new StorageException("Algorithm '%s' is unavailable".formatted(DIGEST_ALGORITHM), exception);
        }
    }

    /**
     * 📊 What one digesting copy established about the bytes.
     *
     * @param sizeBytes number of bytes that actually arrived
     * @param sha256    lower-case hex digest of those bytes
     */
    private record Digested(long sizeBytes, String sha256) {
    }
}
