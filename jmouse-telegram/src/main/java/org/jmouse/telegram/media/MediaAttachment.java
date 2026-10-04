package org.jmouse.telegram.media;

import java.util.Objects;

/**
 * One attachment: what it is, where its bytes come from, and how it is presented.
 *
 * @param kind    how Telegram should treat it — see {@link MediaKind}, where the photo/document
 *                distinction is the one that catches people
 * @param source  where the bytes come from
 * @param caption the text shown with it, or {@code null}. ⚠️ Capped by Telegram at 1024 characters,
 *                against 4096 for a message body — a caption assembled from the same template as a
 *                text notification will occasionally be refused for length alone
 * @param spoiler whether it arrives blurred until tapped
 */
public record MediaAttachment(
        MediaKind   kind,
        MediaSource source,
        String      caption,
        boolean     spoiler
) {

    /** Telegram's caption ceiling, against 4096 for a message body. */
    public static final int CAPTION_LIMIT = 1024;

    public MediaAttachment {
        Objects.requireNonNull(kind, "media kind");
        Objects.requireNonNull(source, "media source");
    }

    public static MediaAttachment of(MediaKind kind, MediaSource source) {
        return new MediaAttachment(kind, source, null, false);
    }

    /** A photo — ⚠️ recompressed by Telegram; use {@link #document} for anything exact. */
    public static MediaAttachment photo(MediaSource source) {
        return of(MediaKind.PHOTO, source);
    }

    /** A file delivered unchanged. */
    public static MediaAttachment document(MediaSource source) {
        return of(MediaKind.DOCUMENT, source);
    }

    public MediaAttachment withCaption(String caption) {
        return new MediaAttachment(kind, source, caption, spoiler);
    }

    public MediaAttachment asSpoiler() {
        return new MediaAttachment(kind, source, caption, true);
    }

    public boolean hasCaption() {
        return caption != null && !caption.isBlank();
    }
}
