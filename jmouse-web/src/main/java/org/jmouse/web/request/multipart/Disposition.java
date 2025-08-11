package org.jmouse.web.request.multipart;

import org.jmouse.core.Bytes;
import org.jmouse.core.MimeParser;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.IntStream;

import static java.nio.charset.StandardCharsets.*;
import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;

/**
 * 📦 Represents a {@code Content-Disposition} header.
 * <p>
 * Encapsulates metadata such as disposition type, field name,
 * file name, charset, file size, and optional timestamps
 * (creation, modification, read).
 *
 * @see <a href="https://tools.ietf.org/html/rfc6266">RFC 6266</a>
 *
 * @param type             Disposition type (e.g. {@code form-data}, {@code attachment})
 * @param name             Field name (for form-data)
 * @param filename         Associated file name
 * @param charset          Charset used for file name encoding
 * @param size             File size in bytes
 * @param creationDate     File creation date
 * @param modificationDate File modification date
 * @param readDate         File last access date
 */
public record Disposition(
        String type,
        String name,
        String filename,
        Charset charset,
        Bytes size,
        ZonedDateTime creationDate,
        ZonedDateTime modificationDate,
        ZonedDateTime readDate
) {

    /**
     * 📝 Builds the {@code Content-Disposition} header string.
     *
     * @return properly formatted header value
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();

        if (this.type != null) {
            builder.append(this.type);
        }

        if (this.name != null) {
            builder.append("; name=\"");
            builder.append(this.name);
            builder.append('\"');
        }

        if (this.filename != null) {
            if (this.charset == null || StandardCharsets.US_ASCII.equals(this.charset)) {
                builder.append("; filename=\"");
                builder.append(Codec.encodeQuoted(this.filename));
                builder.append('\"');
            } else {
                builder.append("; filename=\"");
                builder.append(Codec.encodeRFC2047(this.filename, this.charset));
                builder.append('\"');
                builder.append("; filename*=");
                builder.append(Codec.encodeRFC5987(this.filename, this.charset));
            }
        }

        if (this.size != null) {
            builder.append("; size=");
            builder.append(this.size.toBytes());
        }

        if (this.creationDate != null) {
            builder.append("; creation-date=\"");
            builder.append(RFC_1123_DATE_TIME.format(this.creationDate));
            builder.append('\"');
        }

        if (this.modificationDate != null) {
            builder.append("; modification-date=\"");
            builder.append(RFC_1123_DATE_TIME.format(this.modificationDate));
            builder.append('\"');
        }

        if (this.readDate != null) {
            builder.append("; read-date=\"");
            builder.append(RFC_1123_DATE_TIME.format(this.readDate));
            builder.append('\"');
        }

        return builder.toString();
    }

    /**
     * ✏️ Create a mutable builder from this instance.
     *
     * @return new builder initialized with current values
     */
    public Builder modify() {
        return new Builder(this);
    }

    /**
     * ➕ Create a new empty {@link Builder}.
     *
     * @return new builder
     */
    public static Builder create() {
        return new Builder();
    }

    /**
     * 🔄 Create a builder initialized from an existing disposition.
     *
     * @param disposition existing disposition to copy
     * @return builder with copied values
     */
    public static Builder modify(Disposition disposition) {
        return new Builder(disposition);
    }

    /**
     * 📥 Parse a {@code Content-Disposition} header value.
     *
     * @param content raw header value
     * @return parsed {@link Disposition}
     */
    public static Disposition parse(String content) {
        return new Parser(content).parse();
    }

    /**
     * 📜 Parses a {@code Content-Disposition} header value into a {@link Disposition} object.
     * <p>
     * Supports parsing of attributes such as {@code name}, {@code filename}, {@code filename*},
     * {@code size}, {@code creation-date}, {@code modification-date}, and {@code read-date}.
     * Handles different encoding formats including RFC 5987, RFC 2047 (Q and B encoding),
     * Base64, and quoted-string values.
     * </p>
     */
    public static class Parser {

        /**
         * The raw {@code Content-Disposition} header value.
         */
        private final String content;

        /**
         * Creates a new parser instance.
         *
         * @param content the raw header string to parse
         */
        private Parser(String content) {
            this.content = content;
        }

        /**
         * Parses the {@code Content-Disposition} header into a {@link Disposition} object.
         * <ul>
         *     <li>Extracts the disposition type (e.g., {@code form-data}, {@code attachment}).</li>
         *     <li>Parses parameters such as {@code name}, {@code filename}, and {@code size}.</li>
         *     <li>Decodes encoded filenames using supported RFC formats.</li>
         *     <li>Parses date attributes using {@code RFC_1123_DATE_TIME} format.</li>
         * </ul>
         *
         * @return the parsed {@link Disposition} object
         * @throws IllegalArgumentException if the header contains an unsupported encoding
         *                                  or unknown attribute
         */
        public Disposition parse() {
            List<String> collection = tokenize(content);

            String        type             = collection.getFirst();
            String        name             = null;
            String        filename         = null;
            Charset       charset          = null;
            Bytes         size             = null;
            ZonedDateTime creationDate     = null;
            ZonedDateTime modificationDate = null;
            ZonedDateTime readDate         = null;

            for (int i = 1; i < collection.size(); i++) {
                String token      = collection.get(i);
                int    equalIndex = token.indexOf('=');

                if (equalIndex != -1) {
                    String attribute = token.substring(0, equalIndex).trim();
                    String value     = MimeParser.unquote(token.substring(equalIndex + 1)).trim();

                    switch (attribute) {
                        case "name": {
                            name = value;
                            break;
                        }
                        case "filename*": {
                            int index1 = value.indexOf('\'');
                            int index2 = value.indexOf('\'', index1 + 1);

                            if (index1 != -1 && index2 != -1) {
                                charset = Charset.forName(value.substring(0, index1).trim());

                                if (charset.equals(UTF_8) || charset.equals(ISO_8859_1)) {
                                    filename = Codec.decodeRFC5987(value.substring(index2 + 1), charset);
                                    break;
                                }

                                throw new IllegalArgumentException("UNSUPPORTED ENCODING: " + charset);
                            }

                            filename = Codec.decodeRFC5987(value, StandardCharsets.US_ASCII);

                            break;
                        }
                        case "filename": {

                            if (value.startsWith("=?")) {
                                List<String> parts = Arrays.asList(value.split("\\?"));

                                if (parts.getFirst().equals("=") && parts.getLast().equals("=")) {
                                    List<String> parameters = parts.subList(1, parts.size() - 1);
                                    String       encoded    = parameters.getLast();
                                    String       encoding   = parameters.get(1);

                                    charset = Charset.forName(parameters.getFirst());
                                    filename = switch (encoding) {
                                        case "Q" -> Codec.decodeRFC2047(encoded, charset);
                                        case "B" -> Codec.decodeBase64(encoded, charset);
                                        default -> throw new IllegalArgumentException(
                                                "UNSUPPORTED ENCODING: " + encoding);
                                    };
                                }
                            } else if (value.indexOf('\\') != -1) {
                                filename = Codec.decodeQuoted(value);
                            } else {
                                filename = value;
                            }

                            break;
                        }
                        case "size": {
                            size = Bytes.ofBytes(Long.parseLong(value));
                            break;
                        }
                        case "creation-date": {
                            try {
                                creationDate = ZonedDateTime.parse(value, RFC_1123_DATE_TIME);
                            } catch (DateTimeParseException ignore) {}
                            break;
                        }
                        case "modification-date": {
                            try {
                                modificationDate = ZonedDateTime.parse(value, RFC_1123_DATE_TIME);
                            } catch (DateTimeParseException ignore) {}
                            break;
                        }
                        case "read-date": {
                            try {
                                readDate = ZonedDateTime.parse(value, RFC_1123_DATE_TIME);
                            } catch (DateTimeParseException ignore) {}
                            break;
                        }
                        default: {
                            throw new IllegalArgumentException(
                                    "CONTENT DISPOSITION IS CORRUPTED! UNKNOWN ATTRIBUTE: " + attribute);
                        }
                    }
                }
            }

            return new Disposition(type, name, filename, charset, size, creationDate, modificationDate, readDate);
        }

        /**
         * Splits a {@code Content-Disposition} header value into individual tokens.
         * <p>
         * Handles quoted strings, escaped quotes, and preserves parameter integrity.
         * The first element is typically the disposition type, followed by key-value pairs.
         * </p>
         *
         * @param content the raw header string
         * @return a list of tokens, preserving quoted segments
         */
        public List<String> tokenize(String content) {
            List<String> collection   = new ArrayList<>();
            int          currentIndex = 0;
            int          nextIndex    = content.indexOf(';');

            if (nextIndex > currentIndex) {

                collection.add(content.substring(currentIndex, nextIndex));

                do {
                    boolean quoted  = false;
                    boolean escaped = false;

                    nextIndex++;
                    currentIndex = nextIndex;

                    while (nextIndex < content.length()) {
                        char current = content.charAt(nextIndex);

                        if (current == ';') {
                            if (!quoted) {
                                break;
                            }
                        } else if (!escaped && current == '"') {
                            quoted = !quoted;
                        }

                        escaped = !escaped && current == '\\';

                        nextIndex++;
                    }

                    collection.add(content.substring(currentIndex, nextIndex));

                } while (nextIndex < content.length());
            }

            return collection;
        }

    }

    /**
     * 🔐 Utility class for encoding and decoding header values according to
     * RFC 5987, RFC 2047, and related formats used in HTTP headers
     * such as {@code Content-Disposition}.
     */
    public static class Codec {

        private static final BitSet RFC5987_ALLOWED = new BitSet(256);
        private static final BitSet RFC2047_ALLOWED = new BitSet(256);

        static {
            // RFC5987
            IntStream.rangeClosed('0', '9').forEach(RFC5987_ALLOWED::set);
            IntStream.rangeClosed('a', 'z').forEach(RFC5987_ALLOWED::set);
            IntStream.rangeClosed('A', 'Z').forEach(RFC5987_ALLOWED::set);
            IntStream.of('!', '#', '$', '&', '+', '-', '.', '^', '_', '`', '|', '~')
                    .forEach(RFC5987_ALLOWED::set);
            // RFC2047
            IntStream.rangeClosed(33, 126).forEach(RFC2047_ALLOWED::set);
            RFC2047_ALLOWED.set(61, false); // =
            RFC2047_ALLOWED.set(63, false); // ?
            RFC2047_ALLOWED.set(95, false); // _
        }

        /**
         * ✅ Checks whether a byte is allowed in RFC 5987 encoded values.
         *
         * @param b the byte to check
         * @return {@code true} if allowed, {@code false} otherwise
         */
        public static boolean validateRFC5987(byte b) {
            return RFC5987_ALLOWED.get(b & 0xff);
        }

        /**
         * ✅ Checks whether a byte is allowed in RFC 2047 encoded values.
         *
         * @param b the byte to check
         * @return {@code true} if allowed, {@code false} otherwise
         */
        public static boolean validateRFC2047(byte b) {
            return RFC2047_ALLOWED.get(b & 0xff);
        }

        /**
         * 📝 Decodes a quoted-string value by unescaping
         * backslash-escaped characters.
         *
         * @param encoded the quoted string
         * @return decoded string
         */
        public static String decodeQuoted(String encoded) {
            StringBuilder builder = new StringBuilder();
            int           length  = encoded.length();

            for (int i = 0; i < length; i++) {
                char current = encoded.charAt(i);

                if (current == '\\' && i + 1 < length) {
                    i++;
                    char next = encoded.charAt(i);
                    if (next != '"' && next != '\\') {
                        builder.append(current);
                    }
                    builder.append(next);
                    continue;
                }

                builder.append(current);
            }

            return builder.toString();
        }

        /**
         * 📥 Decodes a filename according to RFC 5987.
         *
         * @param filename the encoded filename
         * @param charset  the charset to decode with
         * @return decoded filename
         */
        public static String decodeRFC5987(String filename, Charset charset) {
            return decode('%', filename, charset, Codec::validateRFC5987);
        }

        /**
         * 📥 Decodes a filename according to RFC 2047 (Q-encoding).
         *
         * @param filename the encoded filename
         * @param charset  the charset to decode with
         * @return decoded filename
         */
        public static String decodeRFC2047(String filename, Charset charset) {
            return decode('=', filename, charset, b -> true);
        }

        /**
         * 📥 Decodes a Base64-encoded string into text.
         *
         * @param encoded the Base64 string
         * @param charset the target charset
         * @return decoded string
         */
        public static String decodeBase64(String encoded, Charset charset) {
            Base64.Decoder decoder = Base64.getDecoder();
            byte[]         decoded = decoder.decode(encoded);
            return new String(decoded, charset);
        }

        /**
         * 📥 Decodes a percent-encoded (or other prefix-based) string.
         *
         * @param prefix     the escape prefix (e.g. '%' or '=')
         * @param encoded    the encoded string
         * @param charset    the target charset
         * @param validator  byte validator to determine allowed raw bytes
         * @return decoded string
         * @throws IllegalArgumentException if the format is invalid
         */
        public static String decode(char prefix, String encoded, Charset charset, Predicate<Byte> validator) {
            byte[]                bytes = encoded.getBytes(US_ASCII);
            ByteArrayOutputStream baos  = new ByteArrayOutputStream();
            int                   index = 0;

            while (index < bytes.length) {
                byte current = bytes[index];

                if (validator.test(current)) {
                    baos.write((char) current);
                    index++;
                    continue;
                }

                if (current == prefix && index + 2 < bytes.length) {
                    int index1 = Character.digit((char) bytes[index + 1], 16);
                    int index2 = Character.digit((char) bytes[index + 2], 16);
                    baos.write((char) ((index1 << 4) | index2));
                    index += 3;
                    continue;
                }

                throw new IllegalArgumentException("INVALID HEADER VALUE FORMAT: " + encoded);
            }

            return baos.toString(charset);
        }

        /**
         * 📤 Encodes a string according to RFC 5987 using percent-encoding.
         *
         * @param decoded the string to encode
         * @param charset the charset to use
         * @return encoded string
         */
        public static String encodeRFC5987(String decoded, Charset charset) {
            StringBuilder builder = new StringBuilder();
            byte[]        bytes   = decoded.getBytes(charset);

            builder.append(charset.name());
            builder.append("''");

            for (byte current : bytes) {
                if (validateRFC5987(current)) {
                    builder.append((char) current);
                    continue;
                }

                builder.append('%');
                builder.append(hexDigit(current >> 4));
                builder.append(hexDigit(current));
            }

            return builder.toString();
        }

        /**
         * 📤 Encodes a string according to RFC 2047 using Q-encoding.
         *
         * @param decoded the string to encode
         * @param charset the charset to use
         * @return encoded string
         */
        public static String encodeRFC2047(String decoded, Charset charset) {
            StringBuilder builder = new StringBuilder();
            byte[]        bytes   = decoded.getBytes(charset);

            builder.append("=?");
            builder.append(charset.name());
            builder.append("?Q?");

            for (byte current : bytes) {
                if (current == 32) {
                    builder.append('_');
                    continue;
                }

                if (validateRFC2047(current)) {
                    builder.append((char) current);
                    continue;
                }

                builder.append('=');
                builder.append(hexDigit(current >> 4));
                builder.append(hexDigit(current));
            }

            builder.append("?=");

            return builder.toString();
        }

        /**
         * 📝 Escapes quotes and backslashes in a string for use
         * inside a quoted-string value.
         *
         * @param encoded the input string
         * @return escaped string
         */
        public static String encodeQuoted(String encoded) {
            if (!encoded.contains("\"") && !encoded.contains("\\")) {
                return encoded;
            }

            StringBuilder builder = new StringBuilder();
            int           length  = encoded.length();

            for (int i = 0; i < length; i++) {
                char current = encoded.charAt(i);

                if (current == '"' || current == '\\') {
                    builder.append('\\');
                }

                builder.append(current);
            }

            return builder.toString();
        }

        /**
         * 🔢 Converts a number (0–15) to its hexadecimal digit (uppercase).
         *
         * @param character the number
         * @return hexadecimal digit character
         */
        public static char hexDigit(int character) {
            return Character.toUpperCase(Character.forDigit(character & 0xF, 16));
        }

    }

    /**
     * 🛠️ Builder for creating and modifying {@link Disposition} instances.
     * <p>
     * Allows fluent configuration of all disposition attributes.
     */
    public static final class Builder {

        private String        type;
        private String        name;
        private String        filename;
        private Charset       charset;
        private Bytes         size;
        private ZonedDateTime creationDate;
        private ZonedDateTime modificationDate;
        private ZonedDateTime readDate;

        /**
         * ✨ Creates a new empty builder.
         */
        public Builder() {
            this(null);
        }

        /**
         * 🔄 Creates a builder initialized from an existing {@link Disposition}.
         *
         * @param disposition source disposition to copy values from, may be {@code null}.
         */
        public Builder(Disposition disposition) {
            if (disposition != null) {
                this.type = disposition.type();
                this.name = disposition.name();
                this.filename = disposition.filename();
                this.charset = disposition.charset();
                this.size = disposition.size();
                this.creationDate = disposition.creationDate();
                this.modificationDate = disposition.modificationDate();
                this.readDate = disposition.readDate();
            }
        }

        /**
         * 🏷️ Sets the disposition type.
         *
         * @param type the type string.
         * @return this builder.
         */
        public Builder type(String type) {
            this.type = type;
            return this;
        }

        /**
         * 📛 Sets the name parameter.
         *
         * @param name the name value.
         * @return this builder.
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * 📂 Sets the filename parameter.
         *
         * @param filename the file name.
         * @return this builder.
         */
        public Builder filename(String filename) {
            this.filename = filename;
            return this;
        }

        /**
         * 🔤 Sets the charset for filename encoding.
         *
         * @param charset the charset.
         * @return this builder.
         */
        public Builder charset(Charset charset) {
            this.charset = charset;
            return this;
        }

        /**
         * 📏 Sets the file size.
         *
         * @param size the file size as {@link Bytes}.
         * @return this builder.
         */
        public Builder size(Bytes size) {
            this.size = size;
            return this;
        }

        /**
         * 📅 Sets the creation date.
         *
         * @param creationDate creation timestamp.
         * @return this builder.
         */
        public Builder creationDate(ZonedDateTime creationDate) {
            this.creationDate = creationDate;
            return this;
        }

        /**
         * ✏️ Sets the modification date.
         *
         * @param modificationDate modification timestamp.
         * @return this builder.
         */
        public Builder modificationDate(ZonedDateTime modificationDate) {
            this.modificationDate = modificationDate;
            return this;
        }

        /**
         * 📖 Sets the read date.
         *
         * @param readDate read timestamp.
         * @return this builder.
         */
        public Builder readDate(ZonedDateTime readDate) {
            this.readDate = readDate;
            return this;
        }

        /**
         * ✅ Builds the {@link Disposition} instance.
         *
         * @return a new {@link Disposition} with configured values.
         */
        public Disposition build() {
            return new Disposition(
                    type,
                    name,
                    filename,
                    charset,
                    size,
                    creationDate,
                    modificationDate,
                    readDate
            );
        }
    }


}
