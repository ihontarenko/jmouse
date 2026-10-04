package org.jmouse.telegram.media;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Where the bytes of an attachment come from.
 *
 * <p>Telegram accepts three genuinely different things in the same field, and the difference matters
 * to the caller rather than only to the transport:
 *
 * <ul>
 *   <li>{@link FileId} — a handle Telegram already holds. Free, instant, and the only sensible way to
 *       send the same picture twice. ⚠️ A file id is <em>not</em> portable between bots: one bot
 *       cannot use another's.</li>
 *   <li>{@link RemoteUrl} — an address Telegram fetches itself. No upload, but the failure moves out
 *       of our reach: if Telegram cannot reach the host, the refusal describes a URL rather than a
 *       network we control.</li>
 *   <li>{@link Bytes} and {@link LocalFile} — a genuine upload, subject to the Bot API's 50 MB
 *       ceiling unless a self-hosted API server is configured.</li>
 * </ul>
 *
 * <p>⚠️ Sealed, so a transport that learns to handle a new source cannot forget one it already had.
 */
public sealed interface MediaSource {

    /** A file Telegram already stores, by the id it handed out on a previous send. */
    record FileId(String value) implements MediaSource {

        public FileId {
            Objects.requireNonNull(value, "file id");
        }
    }

    /** An address Telegram fetches for itself. */
    record RemoteUrl(String value) implements MediaSource {

        public RemoteUrl {
            Objects.requireNonNull(value, "url");
        }
    }

    /**
     * Bytes held in memory.
     *
     * @param fileName    what Telegram should call it; it is what a recipient sees and what a
     *                    document's extension is read from, so it is required rather than generated
     * @param content     the bytes
     * @param contentType the media type, or {@code null} to let the transport infer one
     */
    record Bytes(String fileName, byte[] content, String contentType) implements MediaSource {

        public Bytes {
            Objects.requireNonNull(fileName, "file name");
            Objects.requireNonNull(content, "content");
        }
    }

    /** A file on disk, streamed rather than read into memory. */
    record LocalFile(Path path, String contentType) implements MediaSource {

        public LocalFile {
            Objects.requireNonNull(path, "path");
        }

        public static LocalFile of(Path path) {
            return new LocalFile(path, null);
        }
    }

    /** Whether sending this means uploading bytes, and therefore meeting the size ceiling. */
    default boolean isUpload() {
        return this instanceof Bytes || this instanceof LocalFile;
    }
}
