package org.jmouse.storage;

import org.jmouse.core.MediaType;
import org.jmouse.core.MediaTypeFactory;

import java.util.List;
import java.util.Locale;

/**
 * 🎨 Resolves the content type an object should be stored and served with.
 *
 * <p>Extension-to-type lookup is delegated to {@link MediaTypeFactory} and its bundled
 * {@code mime.types} catalogue rather than reimplemented, and rather than left to
 * {@code Files.probeContentType}, whose answer depends on the host operating system.</p>
 */
public final class ContentTypes {

    /**
     * 📦 What an object is served as when nothing better can be established.
     */
    public static final MediaType DEFAULT = MediaType.APPLICATION_OCTET_STREAM;

    private static final MediaTypeFactory FACTORY   = new MediaTypeFactory();
    private static final String           SEPARATOR = "/";

    private ContentTypes() {
    }

    /**
     * 🔎 Parse a raw content-type header value.
     *
     * @param value raw header value, possibly {@code null} or malformed
     * @return the parsed type, or {@code null} when the value says nothing usable
     */
    public static MediaType parse(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return MediaType.forString(value);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    /**
     * 🔎 Resolve a content type from a filename's extension.
     *
     * @param filename name carrying an extension
     * @return the mapped type, or {@code null} when the extension is absent or unmapped
     */
    public static MediaType forFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return null;
        }

        try {
            List<MediaType> mediaTypes = FACTORY.getMediaTypes(filename);
            return (mediaTypes == null || mediaTypes.isEmpty()) ? null : mediaTypes.getFirst();
        } catch (RuntimeException exception) {
            return null;
        }
    }

    /**
     * 🎯 Establish the content type of content about to be written.
     *
     * <p>Preference order: what the supplier declared, then what the original filename implies,
     * then what the key implies, then {@link #DEFAULT}. The declaration wins because it is the
     * only source that can distinguish two formats sharing an extension.</p>
     *
     * @param content content being written
     * @param key     key it is being written under
     * @return a content type, never {@code null}
     */
    public static MediaType resolve(Content content, StorageKey key) {
        MediaType resolved = content.declaredContentType();

        if (resolved == null) {
            resolved = forFilename(content.originalFilename());
        }

        if (resolved == null) {
            resolved = forFilename(key.value());
        }

        return (resolved != null) ? resolved : DEFAULT;
    }

    /**
     * 🎯 Establish the content type of an already-stored object from its key alone.
     *
     * @param key key of the stored object
     * @return a content type, never {@code null}
     */
    public static MediaType forKey(StorageKey key) {
        MediaType resolved = forFilename(key.value());
        return (resolved != null) ? resolved : DEFAULT;
    }

    /**
     * 🏷️ The bare {@code type/subtype} of a media type, lower-cased and without parameters.
     *
     * <p>What an acceptance policy compares against: {@code text/html; charset=utf-8} and
     * {@code text/html} are the same thing to a rule that blocks markup.</p>
     *
     * @param mediaType the type to reduce, may be {@code null}
     * @return the bare type, or {@code null} when {@code mediaType} is {@code null}
     */
    public static String baseType(MediaType mediaType) {
        if (mediaType == null) {
            return null;
        }

        return (mediaType.getType() + SEPARATOR + mediaType.getSubType()).toLowerCase(Locale.ROOT);
    }
}
