package org.jmouse.json;

/**
 * 🧾 JSON's rule for text inside quotes, read and written in one place: an escape is a backslash and one of {@code " \ / b f n
 * r t}, or {@code u} and four hexadecimal digits; a character below a space is never written as it is.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class JsonTexts {

    private static final int HEXADECIMAL_DIGITS = 4;

    private JsonTexts() {
    }

    /** The text a JSON string writes, its quotes included in {@code written}; refused where it breaks JSON's rule. */
    public static String decode(String written) {
        if (written.length() < 2 || written.charAt(0) != '"' || written.charAt(written.length() - 1) != '"') {
            throw new JsonSyntaxException("a string is written in double quotes: " + written);
        }

        int           last = written.length() - 1;
        StringBuilder text = new StringBuilder(written.length());

        for (int index = 1; index < last; index++) {
            char character = written.charAt(index);

            if (character < 0x20) {
                throw new JsonSyntaxException("a character below a space is escaped in a string, not written as it is");
            }

            if (character != '\\') {
                text.append(character);
                continue;
            }

            char escaped = written.charAt(++index);
            switch (escaped) {
                case '"', '\\', '/' -> text.append(escaped);
                case 'b' -> text.append('\b');
                case 'f' -> text.append('\f');
                case 'n' -> text.append('\n');
                case 'r' -> text.append('\r');
                case 't' -> text.append('\t');
                case 'u' -> {
                    if (index + HEXADECIMAL_DIGITS >= last || !isHexadecimal(written, index + 1)) {
                        throw new JsonSyntaxException("\\u is followed by four hexadecimal digits");
                    }
                    text.append((char) Integer.parseInt(written.substring(index + 1, index + 1 + HEXADECIMAL_DIGITS), 16));
                    index += HEXADECIMAL_DIGITS;
                }
                default -> throw new JsonSyntaxException("\\" + escaped + " is no escape of JSON");
            }
        }

        return text.toString();
    }

    /** {@code text} as a JSON string, quotes included. */
    public static String encode(String text) {
        StringBuilder written = new StringBuilder(text.length() + 2).append('"');

        for (int index = 0; index < text.length(); index++) {
            char character = text.charAt(index);
            switch (character) {
                case '"' -> written.append("\\\"");
                case '\\' -> written.append("\\\\");
                case '\n' -> written.append("\\n");
                case '\r' -> written.append("\\r");
                case '\t' -> written.append("\\t");
                case '\b' -> written.append("\\b");
                case '\f' -> written.append("\\f");
                default -> {
                    if (character < 0x20) {
                        written.append("\\u00").append(Character.forDigit(character >> 4, 16)).append(Character.forDigit(character & 0xF, 16));
                    } else {
                        written.append(character);
                    }
                }
            }
        }

        return written.append('"').toString();
    }

    private static boolean isHexadecimal(String written, int from) {
        for (int index = from; index < from + HEXADECIMAL_DIGITS; index++) {
            if (Character.digit(written.charAt(index), 16) < 0) {
                return false;
            }
        }
        return true;
    }
}
