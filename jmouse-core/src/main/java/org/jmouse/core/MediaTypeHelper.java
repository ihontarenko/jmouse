package org.jmouse.core;

/**
 * 🎨 Helper utilities for working with {@link MediaType}s and multipart boundaries.
 *
 * <p>Provides:</p>
 * <ul>
 *   <li>📑 Lookup of media types from file paths</li>
 *   <li>🧩 Generation of multipart/form-data boundaries</li>
 * </ul>
 */
public final class MediaTypeHelper {

    /**
     * 🏭 Shared media type factory.
     */
    private static final MediaTypeFactory FACTORY = new MediaTypeFactory();

    /**
     * 🔑 Random multipart boundary generator.
     */
    private static final MultipartBoundaryGenerator GENERATOR = new MultipartBoundaryGenerator();

    /**
     * 🏷️ Title prefix for generated boundaries.
     */
    public static final String TITLE = "jMouse";

    /**
     * 🚫 Utility class: prevent instantiation.
     */
    private MediaTypeHelper() {
    }

    /**
     * 📑 Resolve the {@link MediaType} for a given file path.
     *
     * @param filepath file name or path (e.g. {@code image.png})
     * @return resolved {@link MediaType}, or {@code null} if unknown
     */
    public static MediaType getMediaType(String filepath) {
        return FACTORY.getMediaType(filepath);
    }

    /**
     * 🔁 Resolve a preferred file extension for the given {@link MimeType}.
     *
     * <p>This is a reverse lookup using a lazily populated cache derived from
     * the extension → media types registry.</p>
     *
     * <p><b>Notes:</b></p>
     * <ul>
     *   <li>If multiple extensions are associated with the same {@link MimeType},
     *       the last registered extension wins.</li>
     *   <li>If no extension is registered for the given type, {@code null} is returned.</li>
     * </ul>
     *
     * @param type MIME type to resolve
     * @return file extension without dot (e.g. {@code "png"}), or {@code null} if unknown
     */
    public static String getExtensionFor(MimeType type) {
        return FACTORY.getReversedCache().get(type);
    }

    /**
     * 🧩 Generate a multipart boundary string.
     *
     * @param includeTitle whether to prefix the boundary with {@code "jMouse_"}
     * @return random boundary string
     */
    public static String generateMultipartBoundary(boolean includeTitle) {
        return includeTitle ? TITLE + "_" + GENERATOR.generate() : GENERATOR.generate();
    }

}
