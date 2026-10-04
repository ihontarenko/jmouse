package org.jmouse.telegram;

/**
 * How Telegram should read the formatting in a message body.
 *
 * @see MarkupEscaper
 */
public enum ParseMode {

    /**
     * No formatting at all. Every character is literal and nothing needs escaping.
     *
     * <p>⚠️ The right default. A notification assembled from data — a film title, a person's name, a
     * file path — contains whatever those contain, and the commonest formatting bug in a Telegram
     * integration is a send that fails on punctuation nobody chose.
     */
    NONE(null),

    /**
     * Telegram's MarkdownV2.
     *
     * <p>⚠️ Eighteen reserved characters, all of which must be escaped <em>everywhere</em> in the
     * text and not only where formatting was intended. An unescaped {@code (} in a film title —
     * {@code Blade Runner (1982)} — fails the whole send with a parse error. See {@link MarkupEscaper}.
     */
    MARKDOWN_V2("MarkdownV2"),

    /**
     * Telegram's HTML subset: {@code b i u s a code pre blockquote tg-spoiler}.
     *
     * <p>Usually the easier of the two to generate, because only three characters are special and the
     * escaping rule does not change inside an entity.
     */
    HTML("HTML");

    private final String wireValue;

    ParseMode(String wireValue) {
        this.wireValue = wireValue;
    }

    /** What goes on the wire as {@code parse_mode}, or {@code null} for {@link #NONE}. */
    public String wireValue() {
        return wireValue;
    }

    public boolean isFormatted() {
        return wireValue != null;
    }
}
