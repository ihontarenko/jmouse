package org.jmouse.telegram.media;

/**
 * What Telegram should treat an attachment as.
 *
 * <p>⚠️ This is a presentation decision, not a file-type one, and the two are routinely confused. The
 * same JPEG sent as {@link #PHOTO} is recompressed by Telegram and shown inline; sent as
 * {@link #DOCUMENT} it arrives byte-for-byte as a downloadable file. For a screenshot a person is
 * meant to read, the second is usually what was wanted and the first is what was written.
 */
public enum MediaKind {

    /** ⚠️ Shown inline and <strong>recompressed</strong> by Telegram. Never use it for anything exact. */
    PHOTO("sendPhoto", "photo"),

    /** A file, delivered unchanged. The right choice for a screenshot, a report, a log, an export. */
    DOCUMENT("sendDocument", "document"),

    VIDEO("sendVideo", "video"),

    AUDIO("sendAudio", "audio"),

    /** A silent looping video — what Telegram calls a GIF, whatever the container actually is. */
    ANIMATION("sendAnimation", "animation"),

    /** A voice message: an audio file presented as a waveform rather than as a track. */
    VOICE("sendVoice", "voice"),

    /** A round video note. */
    VIDEO_NOTE("sendVideoNote", "video_note");

    private final String method;
    private final String field;

    MediaKind(String method, String field) {
        this.method = method;
        this.field  = field;
    }

    /** The Bot API method that sends this kind. */
    public String method() {
        return method;
    }

    /** The request field the payload goes in. */
    public String field() {
        return field;
    }

    /**
     * Whether Telegram allows this kind in a media group (an album).
     *
     * <p>Only photos, videos, documents and audio may be grouped, and ⚠️ a group may not mix
     * incompatible kinds — audio groups with audio, documents with documents. A transport refuses a
     * malformed group rather than letting Telegram describe it.
     */
    public boolean groupable() {
        return this == PHOTO || this == VIDEO || this == DOCUMENT || this == AUDIO;
    }
}
