package org.jmouse.telegram;

/**
 * Makes an arbitrary string safe to drop into a formatted message.
 *
 * <h2>⚠️ Why this is the library's job and not the caller's</h2>
 *
 * <p>MarkdownV2 reserves eighteen characters, and Telegram requires every one of them to be escaped
 * <em>anywhere</em> in the text — not only where formatting was meant. The characters are ordinary
 * punctuation, so the failure arrives through data rather than through code: a release year in
 * brackets, a hyphen in a surname, a full stop at the end of a sentence. {@code Blade Runner (1982)}
 * fails the entire send with a parse error, and the code that composed it is obviously correct.
 *
 * <p>A caller asked to remember this will forget, and will forget differently in each product. So it
 * lives here, and the composition helpers on {@link MessageDraft} apply it.
 *
 * <p>⚠️ <strong>Escape the values, not the finished message.</strong> Escaping a string that already
 * contains intentional formatting destroys the formatting — that is what it is for. The shape that
 * works is to build the markup and escape each interpolated value as it goes in:
 *
 * <pre>{@code
 * String text = "*%s* was opened by %s".formatted(
 *         MarkupEscaper.markdownV2(film.title()),
 *         MarkupEscaper.markdownV2(person.name()));
 * }</pre>
 */
public final class MarkupEscaper {

    /**
     * The eighteen characters MarkdownV2 reserves, in the order Telegram's own documentation lists
     * them. Kept as a string rather than a set so the count is visible to a reader checking it.
     */
    private static final String MARKDOWN_V2_RESERVED = "_*[]()~`>#+-=|{}.!";

    private MarkupEscaper() {
    }

    /**
     * Escapes every MarkdownV2 reserved character with a backslash.
     *
     * @param value the raw value; {@code null} becomes an empty string, because a missing field in a
     *              notification should read as nothing rather than as the word "null"
     */
    public static String markdownV2(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }

        StringBuilder escaped = new StringBuilder(value.length() + 8);

        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);

            if (MARKDOWN_V2_RESERVED.indexOf(character) >= 0) {
                escaped.append('\\');
            }

            escaped.append(character);
        }

        return escaped.toString();
    }

    /**
     * Escapes the three characters Telegram's HTML subset reserves.
     *
     * <p>Only {@code &}, {@code <} and {@code >} — quotes are not special outside an attribute, and
     * Telegram's subset has no attributes a caller writes by hand apart from {@code href}, which is a
     * URL and is escaped by the same rule.
     */
    public static String html(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }

        return value.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;");
    }

    /**
     * Escapes for whichever mode is in force, so composition code does not branch.
     *
     * <p>{@link ParseMode#NONE} returns the value untouched — there is nothing to escape when nothing
     * is parsed, and escaping anyway would put visible backslashes in front of every full stop.
     */
    public static String forMode(ParseMode mode, String value) {
        return switch (mode) {
            case MARKDOWN_V2 -> markdownV2(value);
            case HTML        -> html(value);
            case NONE        -> value == null ? "" : value;
        };
    }
}
