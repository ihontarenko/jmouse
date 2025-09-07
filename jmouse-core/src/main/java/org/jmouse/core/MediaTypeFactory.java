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
     * ⚡ Cached mapping of file extension → media types.
     */
    private static final Map<String, List<MediaType>> CACHE = parseMIMETypes();

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
}
