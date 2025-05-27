package org.jmouse.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class for parsing MIME type strings into {@link MimeType} instances.
 * <p>This parser supports comma-separated lists of MIME types and individual
 * type/subtype pairs with optional parameters.</p>
 */
public class MimeParser {

    /**
     * Parse a comma-separated list of MIME type values.
     * <p>Example:
     * <pre>{@code
     * List<MimeType> types = MimeParser.parseMimeTypes(
     *     "application/json; charset=UTF-8, text/html"
     * );
     * }</pre>
     * </p>
     *
     * @param mimeTypes the raw header value containing one or more MIME types
     * @return a list of parsed {@link MimeType} objects; never null
     * @throws InvalidMimeTypeException if any entry is null, blank, or malformed
     */
    public static List<MimeType> parseMimeTypes(String mimeTypes) {
        List<MimeType> types = new ArrayList<>(8);

        if (mimeTypes.indexOf(',') > -1) {
            for (String mimeType : mimeTypes.split(",")) {
                types.add(parseMimeType(mimeType));
            }
        } else {
            types.add(parseMimeType(mimeTypes));
        }

        return types;
    }

    /**
     * Parse a single MIME type string into a {@link MimeType} instance.
     *
     * @param mimeType the raw MIME type string (e.g. "text/html; charset=UTF-8")
     * @return the parsed {@link MimeType}
     * @throws InvalidMimeTypeException if the input is null, blank,
     *         missing '/', missing subtype, or parameter malformed
     */
    public static MimeType parseMimeType(String mimeType) {
        if (mimeType == null || mimeType.isBlank()) {
            throw new InvalidMimeTypeException(mimeType, "'mimeType' must not be empty");
        }

        mimeType = mimeType.trim();

        int    index    = mimeType.indexOf(';');
        String rawType  = (index >= 0 ? mimeType.substring(0, index) : mimeType).trim();
        int    subIndex = rawType.indexOf('/');

        if (subIndex == -1) {
            throw new InvalidMimeTypeException(mimeType, "does not contain '/'");
        }

        if (subIndex == rawType.length() - 1) {
            throw new InvalidMimeTypeException(mimeType, "does not contain subtype after '/'");
        }

        Map<String, String> parameters = new HashMap<>();
        String              type       = rawType.substring(0, subIndex);
        String              subtype    = rawType.substring(subIndex + 1);

        // Parse parameters if present
        while (index != -1 && index < mimeType.length()) {
            int nextIndex = index + 1;

            while (nextIndex < mimeType.length() &&  mimeType.charAt(nextIndex) != ';') {
                nextIndex++;
            }

            String parameter = mimeType.substring(index + 1, nextIndex);

            if (!parameter.isEmpty()) {
                int    eqIndex   = parameter.indexOf('=');

                if (eqIndex == -1) {
                    throw new InvalidMimeTypeException(
                            mimeType, "parameter '" + parameter + "' is not in 'name=value' format");
                }

                String attribute = parameter.substring(0, eqIndex).trim();
                String value     = parameter.substring(eqIndex + 1).trim();
                parameters.put(attribute, unquote(value));

            }

            index = nextIndex;
        }

        return new MimeType(type, subtype, parameters);
    }

    /**
     * Remove matching surrounding quotes from the given string.
     * <p>If the value starts and ends with the same quote character—either
     * double-quote {@code "} or single-quote {@code '}—those quotes are removed.
     * Otherwise, the original string is returned unchanged.</p>
     *
     * <pre>{@code
     * MimeParser.unquote("\"hello\"");   // returns "hello"
     * MimeParser.unquote("'world'");     // returns "world"
     * MimeParser.unquote("noquotes");    // returns "noquotes"
     * MimeParser.unquote("\"mismatch'"); // returns "\"mismatch'"
     * }</pre>
     *
     * @param value the input string, possibly surrounded by single or double quotes
     * @return the unquoted string if both ends have the same quote character; otherwise the original value
     * @throws NullPointerException if {@code value} is {@code null}
     */
    public static String unquote(String value) {
        if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

}
