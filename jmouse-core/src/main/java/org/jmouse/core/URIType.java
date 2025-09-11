package org.jmouse.core;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.jmouse.core.CharMask.builder;
import static org.jmouse.core.CharMask.ofRanges;

/**
 * 🧭 RFC 3986 component-aware character policy + encoder.
 */
public enum URIType {

    SCHEME,
    AUTHORITY,
    USER_INFO,
    HOST_IPV4,
    HOST_IPV6,
    PORT,
    PATH,
    PATH_SEGMENT,
    QUERY,
    QUERY_PARAMETER,   // forbids '=' and '&' as they are pair separators
    FRAGMENT,
    URI;           // safest: only unreserved

    private static final CharMask ALPHA_CHARACTERS  = ofRanges('a', 'z', 'A', 'Z');
    private static final CharMask DIGIT_CHARACTERS  = ofRanges('0', '9');
    private static final CharMask GENERIC_DELIMITER = builder()
            .set(':', '/', '?', '#', '[', ']', '@').build();
    private static final CharMask SUB_DELIMITER     = builder()
            .set('!', '$', '&', '\'', '(', ')', '*', '+', ',', ';', '=').build();
    private static final CharMask HEX_CHARACTERS    = DIGIT_CHARACTERS
            .merge(CharMask.ofRanges('A', 'F', 'a', 'f'));
    private static final CharMask UNRESERVED        = ALPHA_CHARACTERS
            .merge(DIGIT_CHARACTERS).mutate().set('~', '-', '_', '.').build();
    private static final CharMask PCHAR             = UNRESERVED
            .merge(SUB_DELIMITER).mutate().set(':', '@').build();
    private static final CharMask RESERVED          = GENERIC_DELIMITER
            .merge(SUB_DELIMITER);

    private static final char[]   HEX               = "0123456789ABCDEF".toCharArray();

    private static boolean isAlpha(int character) {
        return ALPHA_CHARACTERS.contains((char) character);
    }

    private static boolean isDigit(int character) {
        return DIGIT_CHARACTERS.contains((char) character);
    }

    /**
     * gen-delimiters = : / ? # [ ] @
     */
    @SuppressWarnings("unused")
    private static boolean isGenericDelimiter(int character) {
        return GENERIC_DELIMITER.contains((char) character);
    }

    /**
     * sub-delimiters = ! $ & ' ( ) * + , ; =
     */
    private static boolean isSubDelimiter(int character) {
        return SUB_DELIMITER.contains((char) character);
    }

    /**
     * reserved = gen-delimiters / sub-delimiters
     */
    @SuppressWarnings("unused")
    private static boolean isReserved(int character) {
        return RESERVED.contains((char) character);
    }

    /**
     * unreserved = ALPHA / DIGIT / - . _ ~
     */
    private static boolean isUnreserved(int character) {
        return UNRESERVED.contains((char) character);
    }

    /**
     * pchar = unreserved / sub-delimiters / ":" / "@"
     */
    private static boolean isPchar(int character) {
        return PCHAR.contains((char) character);
    }

    private static boolean isNextEncoded(CharSequence sequence, int index) {
        // expects two hex chars after '%'
        if (index + 1 >= sequence.length()) {
            return false;
        }

        char a = sequence.charAt(index);
        char b = sequence.charAt(index + 1);

        return isHex(a) && isHex(b);
    }

    private static boolean isHex(char character) {
        return HEX_CHARACTERS.contains(character);
    }

    private static int hexValue(char c) {
        if (c >= '0' && c <= '9') {
            return c - '0';
        }

        if (c >= 'A' && c <= 'F') {
            return 10 + (c - 'A');
        }

        return 10 + (c - 'a'); // c in [a-f] guaranteed by isHex()
    }

    /**
     * ✅ Is the code point allowed verbatim in this component?
     */
    public boolean isAllowed(int character) {
        return switch (this) {
            case SCHEME -> isAlpha(character) || isDigit(character) || character == '+' || character == '-' || character == '.';
            case AUTHORITY, PATH_SEGMENT -> isPchar(character);
            case USER_INFO -> isUnreserved(character) || isSubDelimiter(character) || character == ':';
            case HOST_IPV4 -> isUnreserved(character) || isSubDelimiter(character);
            case HOST_IPV6 -> isUnreserved(character) || isSubDelimiter(character) || character == '[' || character == ']' || character == ':';
            case PORT -> isDigit(character);
            case PATH -> isPchar(character) || character == '/';
            case QUERY, FRAGMENT -> isPchar(character) || character == '/' || character == '?';
            case QUERY_PARAMETER -> (character != '=' && character != '&') && (isPchar(character) || character == '/' || character == '?');
            case URI -> isUnreserved(character);
        };
    }

    /**
     * 🔐 Percent-encode characters not allowed in this component.
     *
     * @param string               input text
     * @param charset              charset used for byte mapping (UTF-8 recommended)
     * @param preservePercent if true, existing "%HH" sequences are kept as-is when valid
     */
    public String encode(CharSequence string, Charset charset, boolean preservePercent) {
        if (string == null || string.isEmpty()) {
            return "";
        }

        StringBuilder builder = new StringBuilder(string.length() + 16);

        for (int i = 0; i < string.length(); ) {

            int point = Character.codePointAt(string, i);

            i += Character.charCount(point);

            if (preservePercent && point == '%' && isNextEncoded(string, i)) {
                // keep "%HH" verbatim
                builder.append('%').append(string.charAt(i)).append(string.charAt(i + 1));
                i += 2;
                continue;
            }

            if (isAllowed(point)) {
                builder.appendCodePoint(point);
            } else {
                for (byte character : new String(Character.toChars(point)).getBytes(charset)) {
                    int hi = (character >> 4) & 0xf;
                    int lo = (character & 0xf);
                    builder.append('%').append(HEX[hi]).append(HEX[lo]);
                }
            }
        }

        return builder.toString();
    }

    /**
     * ♻️ Percent-decode this component.
     *
     * @param string        input text
     * @param charset       charset for mapping bytes to chars (UTF-8 recommended)
     */
    public String normalize(CharSequence string, Charset charset) {
        if (string == null || string.isEmpty()) {
            return "";
        }

        StringBuilder builder = new StringBuilder(string.length());

        final int size = string.length();

        for (int i = 0; i < size; ) {
            int point = Character.codePointAt(string, i);
            int step  = Character.charCount(point);

            if (point != '%') {
                builder.appendCodePoint(point);
                i += step;
                continue;
            }

            // Collect contiguous %HH groups into a byte buffer
            int k = i;
            // verify first %HH exists
            if (k + 2 >= size || string.charAt(k) != '%' || !isHex(string.charAt(k + 1)) || !isHex(string.charAt(k + 2))) {
                // malformed %, keep literal
                builder.append('%');
                i += 1;
                continue;
            }

            // We will decode groups like %E2%82%AC into bytes [0xE2,0x82,0xAC]
            ByteArrayOutputStream buffer = new ByteArrayOutputStream(8);
            while (k + 2 < size && string.charAt(k) == '%' && isHex(string.charAt(k + 1)) && isHex(string.charAt(k + 2))) {
                int hi = hexValue(string.charAt(k + 1));
                int lo = hexValue(string.charAt(k + 2));
                buffer.write((hi << 4) | lo);
                k += 3;
            }

            builder.append(buffer.toString(charset));

            i = k;
        }

        return builder.toString();
    }

    /**
     * ♻️ UTF-8 + RFC-safe (decode only UNRESERVED).
     */
    public String normalize(CharSequence string) {
        return normalize(string, UTF_8);
    }

    /**
     * 🔐 UTF-8 convenience.
     */
    public String encode(CharSequence string) {
        return encode(string, UTF_8, true);
    }

}
