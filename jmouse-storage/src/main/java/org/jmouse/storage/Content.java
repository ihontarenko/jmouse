package org.jmouse.storage;

import org.jmouse.core.MediaType;
import org.jmouse.storage.exception.StorageException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 📦 Everything the storage layer needs to ingest a piece of content, and nothing framework-shaped.
 *
 * <p>No multipart type, no servlet type, no {@code Resource} — so the same call works from a Spring
 * controller, a jMouse handler, a scheduled import and a plain test. Adapting a framework's upload
 * type into this record is the framework adapter's job, not the library's.</p>
 *
 * <p>Both {@code originalFilename} and {@code declaredContentType} are <em>claims made by whoever
 * supplied the bytes</em>. They are what an acceptance policy inspects; they are never trusted as
 * facts about the bytes themselves.</p>
 *
 * @param originalFilename    name the content arrived under (never blank)
 * @param declaredContentType content type the supplier claimed, or {@code null} when it claimed none
 * @param declaredSize        size the supplier claimed, or {@link #UNKNOWN_SIZE} when unknown
 * @param stream              opens the bytes on demand
 */
public record Content(String originalFilename, MediaType declaredContentType, long declaredSize,
                      ContentStream stream) {

    /**
     * ❓ Declared size of content whose length is not known up front — a remote server that omits
     * {@code Content-Length}, a generated stream. Callers re-check against the real size once the
     * bytes have actually arrived.
     */
    public static final long UNKNOWN_SIZE = -1L;

    /**
     * 🏗️ Validate the invariants every backend relies on.
     */
    public Content {
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new StorageException("Content must carry an original filename.");
        }

        if (stream == null) {
            throw new StorageException("Content must carry a stream supplier.");
        }
    }

    /**
     * 📦 Content whose type was declared as a raw header value.
     *
     * <p>An unparseable declaration is treated as no declaration at all, which is what a lying
     * client produces and is not worth failing the whole upload over — the acceptance policy and
     * the extension check still apply.</p>
     *
     * @param originalFilename    name the content arrived under
     * @param declaredContentType raw content-type header value, possibly {@code null} or nonsense
     * @param declaredSize        declared size, or {@link #UNKNOWN_SIZE}
     * @param stream              opens the bytes on demand
     * @return the content
     */
    public static Content of(String originalFilename, String declaredContentType, long declaredSize,
                             ContentStream stream) {
        return new Content(originalFilename, ContentTypes.parse(declaredContentType), declaredSize, stream);
    }

    /**
     * 📦 Content already held in memory.
     *
     * @param originalFilename    name the content arrived under
     * @param declaredContentType content type the supplier claimed, or {@code null}
     * @param bytes               the content itself
     * @return the content
     */
    public static Content ofBytes(String originalFilename, MediaType declaredContentType, byte[] bytes) {
        return new Content(originalFilename, declaredContentType, bytes.length,
                           () -> new ByteArrayInputStream(bytes));
    }

    /**
     * 📦 Content read from a file on disk, taking its name and size from the file itself.
     *
     * @param path the file to ingest
     * @return the content
     */
    public static Content ofFile(Path path) {
        long size;

        try {
            size = Files.size(path);
        } catch (IOException exception) {
            throw new StorageException("Cannot read size of '%s'".formatted(path), exception);
        }

        return new Content(path.getFileName().toString(), null, size, () -> Files.newInputStream(path));
    }

    /**
     * 📄 The lower-cased extension of {@link #originalFilename}, without its dot.
     *
     * @return the extension, or an empty string when the filename carries none
     */
    public String extension() {
        return Filenames.extensionOf(originalFilename);
    }

    /**
     * 📏 Whether the supplier told us how large the content is.
     *
     * @return {@code true} when {@link #declaredSize} is meaningful
     */
    public boolean hasDeclaredSize() {
        return declaredSize >= 0;
    }
}
