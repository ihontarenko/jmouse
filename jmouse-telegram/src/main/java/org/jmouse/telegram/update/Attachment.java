package org.jmouse.telegram.update;

import java.util.Objects;

/**
 * Something attached to a message that arrived: a document, a photo, a voice note.
 *
 * <p>⚠️ <strong>A reference, not the bytes.</strong> Telegram sends the {@code file_id} and keeps the file;
 * fetching it is a separate call that a handler makes only when it wants the content — see
 * {@link org.jmouse.telegram.TelegramGateway#describeFile(String)}. A handler that only forwards a file never
 * downloads it.
 *
 * @param fileId       what fetching or re-sending it takes; valid for this bot only
 * @param fileUniqueId stable across bots and over time — what to deduplicate by, never what to fetch with
 * @param kind         which member of the message it came from
 * @param fileName     the name the sender's client gave it, or {@code null} — ⚠️ a photo never has one
 * @param mimeType     the type the sender's client claimed, or {@code null}; a claim, not a fact
 * @param sizeBytes    the size Telegram reports, or {@link #UNKNOWN_SIZE}
 */
public record Attachment(
        String fileId,
        String fileUniqueId,
        Kind   kind,
        String fileName,
        String mimeType,
        long   sizeBytes
) {

    /** A size Telegram did not report. */
    public static final long UNKNOWN_SIZE = -1L;

    /** The member of Telegram's {@code Message} an attachment arrived in. */
    public enum Kind {
        PHOTO, DOCUMENT, VIDEO, AUDIO, ANIMATION, VOICE, VIDEO_NOTE
    }

    public Attachment {
        Objects.requireNonNull(fileId, "file id");
        Objects.requireNonNull(kind, "kind");
    }

    /**
     * A name to store it under: the sender's own, or one made from the kind and the type — a photo arrives
     * nameless, and storage refuses content without a name.
     */
    public String fileNameOrDefault() {
        if (fileName != null && !fileName.isBlank()) {
            return fileName;
        }

        String stem = kind.name().toLowerCase() + "-" + (fileUniqueId == null ? fileId : fileUniqueId);

        return switch (kind) {
            case PHOTO -> stem + ".jpg";
            case VOICE -> stem + ".ogg";
            case VIDEO, VIDEO_NOTE, ANIMATION -> stem + ".mp4";
            default -> stem;
        };
    }
}
