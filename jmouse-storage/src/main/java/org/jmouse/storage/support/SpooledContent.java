package org.jmouse.storage.support;

import org.jmouse.storage.Content;
import org.jmouse.storage.exception.StorageException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 🥄 Content read once onto disk, so it can be measured, digested, and then read again.
 *
 * <p>A stream answers only one question, and only once: what bytes. Two things need more than
 * that. A content-addressed layout has to know the digest <em>before</em> it can say where the
 * content goes, and deduplication has to ask the registry whether these exact bytes are already
 * stored before deciding to store them at all. Both need the bytes twice, and content arriving
 * over a network may not come twice.</p>
 *
 * <p>So it lands on disk once, digested in the same pass, and everything afterwards reads from
 * there. The cost is one local round-trip; the alternative is holding an arbitrary upload in
 * memory, which is not an alternative.</p>
 *
 * <p><strong>Close it.</strong> The spool is a real file and nothing else will remove it.</p>
 */
public final class SpooledContent implements AutoCloseable {

    private static final String TEMPORARY_PREFIX = "jmouse-storage-spool-";
    private static final String TEMPORARY_SUFFIX = ".part";

    private final Path     spool;
    private final Content  content;
    private final Digested digested;

    private SpooledContent(Path spool, Content content, Digested digested) {
        this.spool    = spool;
        this.content  = content;
        this.digested = digested;
    }

    /**
     * 🥄 Read content onto disk, digesting as it goes.
     *
     * @param content content to spool
     * @return the spooled content, re-readable and digested
     * @throws StorageException when the bytes cannot be read or written
     */
    public static SpooledContent of(Content content) {
        Path spool = null;

        try {
            spool = Files.createTempFile(TEMPORARY_PREFIX, TEMPORARY_SUFFIX);

            Digested digested = ContentDigests.copyTo(content, spool);
            Path     source   = spool;

            Content spooled = new Content(content.originalFilename(), content.declaredContentType(),
                                          digested.sizeBytes(), () -> Files.newInputStream(source));

            return new SpooledContent(spool, spooled, digested);
        } catch (IOException | RuntimeException exception) {
            TemporaryFiles.discard(spool);
            throw new StorageException("Failed to spool '%s': %s"
                                               .formatted(content.originalFilename(), exception.getMessage()),
                                       exception);
        }
    }

    /**
     * 📦 The content, now backed by the spool and readable as many times as needed.
     *
     * <p>Its declared size is the real one — the bytes have arrived, so there is nothing left to
     * take a client's word for.</p>
     *
     * @return the re-readable content
     */
    public Content content() {
        return content;
    }

    /**
     * 🔐 Digest of the bytes, established during the spool rather than by a second pass.
     *
     * @return lower-case hex SHA-256
     */
    public String sha256() {
        return digested.sha256();
    }

    /**
     * 📏 How many bytes actually arrived.
     *
     * @return the real size
     */
    public long sizeBytes() {
        return digested.sizeBytes();
    }

    @Override
    public void close() {
        TemporaryFiles.discard(spool);
    }
}
