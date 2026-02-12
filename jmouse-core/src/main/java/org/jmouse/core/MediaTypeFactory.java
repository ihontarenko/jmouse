package org.jmouse.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 🎨 Factory for resolving {@link MediaType} instances
 * from file extensions using a {@code mime.types} mapping.
 *
 * <p>Reads mappings from resource
 * {@code /org/jmouse/core/mime.types} on initialization.</p>
 */
public final class MediaTypeFactory {

    /**
     * 🔤 Token delimiters in mime.types file.
     */
    public static final String DELIMITERS = " \t\n\r\f";

    /**
     * 📂 Resource path to default mime type definitions.
     */
    private static final String MIME_TYPES_FILE = "/org/jmouse/core/mime.types";

    /**
     * ⚡ Cached mapping of file extension → supported media types.
     *
     * <p>This map is populated eagerly at class initialization time and represents
     * the authoritative source of MIME type associations.</p>
     *
     * <p>Keys are file extensions (without dot), values are ordered lists of
     * {@link MediaType}s associated with that extension.</p>
     */
    private static final Map<String, List<MediaType>> CACHE = parseMIMETypes();

    /**
     * Mutable reverse cache used by {@link #getReversedCache()}.
     *
     * <p>This map is populated lazily and is intended as a derived view of {@link #CACHE}.</p>
     *
     * <p><b>⚠ Note:</b> This map is mutable and not synchronized.</p>
     */
    private static final Map<MimeType, String> REVERSED_CACHE = new HashMap<>();

    /**
     * 🏗️ Construct a new factory (re-parses mime types).
     */
    public MediaTypeFactory() {
        parseMIMETypes();
    }

    /**
     * 📖 Parse the {@code mime.types} file into a lookup map.
     *
     * <ul>
     *   <li>Ignores blank lines and comments (#).</li>
     *   <li>Each line: first token = MIME type, rest = extensions.</li>
     *   <li>Populates mapping extension → {@link MediaType} list.</li>
     * </ul>
     *
     * @return mapping of extensions to supported media types
     */
    private static Map<String, List<MediaType>> parseMIMETypes() {
        Map<String, List<MediaType>> extensions = new HashMap<>();
        InputStream                  stream     = MediaTypeFactory.class.getResourceAsStream(MIME_TYPES_FILE);

        if (stream == null) {
            throw new IllegalStateException(MIME_TYPES_FILE + " not found");
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.US_ASCII))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank() || line.startsWith("#")) {
                    continue;
                }

                List<String> tokens    = tokenize(line);
                MediaType    mediaType = new MediaType(MimeParser.parseMimeType(tokens.getFirst()));

                List<String> types = tokens.subList(1, tokens.size());
                for (String type : types) {
                    extensions.computeIfAbsent(type, k -> new ArrayList<>()).add(mediaType);
                }
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to open " + MIME_TYPES_FILE, exception);
        }

        return extensions;
    }

    /**
     * ✂️ Tokenize a line from {@code mime.types}.
     *
     * @param line raw line
     * @return list of non-blank tokens
     */
    private static List<String> tokenize(String line) {
        StringTokenizer tokenizer = new StringTokenizer(line, DELIMITERS);
        List<String>    tokens    = new ArrayList<>();

        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken().trim();
            if (!token.isBlank()) {
                tokens.add(token);
            }
        }

        return tokens;
    }

    /**
     * 🎯 Resolve first matching media type for the given file name.
     *
     * @param fileName name of file with extension
     * @return first matching {@link MediaType}
     * @throws IllegalArgumentException if no extension is present
     */
    public MediaType getMediaType(String fileName) {
        return getMediaTypes(fileName).getFirst();
    }

    /**
     * 📦 Resolve all matching media types for the given file name.
     *
     * @param fileName name of file with extension
     * @return list of matching media types (may be {@code null} if none found)
     * @throws IllegalArgumentException if no extension is present
     */
    public List<MediaType> getMediaTypes(String fileName) {
        String extension = fileName.substring(fileName.lastIndexOf('.') + 1);

        if (extension.isBlank()) {
            throw new IllegalArgumentException("File name must have extension: " + fileName);
        }

        return CACHE.get(extension);
    }

    /**
     * Reverse lookup cache mapping {@link MimeType} → file extension.
     *
     * <p>This cache is derived from {@link #CACHE} and allows resolving a preferred
     * file extension for a given {@link MimeType}.</p>
     *
     * <p><b>Initialization:</b></p>
     * <ul>
     *   <li>The map is populated lazily on first access.</li>
     *   <li>All {@link MediaType}s associated with an extension are registered.</li>
     *   <li>If the same {@link MimeType} appears under multiple extensions,
     *       the last one encountered wins.</li>
     * </ul>
     *
     * <p><b>⚠ Thread-safety note:</b><br>
     * This implementation is <em>not thread-safe</em>. Concurrent access during
     * the first initialization may lead to race conditions.
     * Consider eager initialization or synchronization if used in multi-threaded contexts.</p>
     *
     * @return mutable map of {@link MimeType} to file extension
     */
    public Map<MimeType, String> getReversedCache() {
        if (REVERSED_CACHE.isEmpty()) {
            for (Map.Entry<String, List<MediaType>> entry : CACHE.entrySet()) {
                String extension = entry.getKey();
                for (MediaType mediaType : entry.getValue()) {
                    REVERSED_CACHE.put(mediaType, extension);
                }
            }
        }
        return REVERSED_CACHE;
    }
}
