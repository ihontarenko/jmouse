package org.jmouse.el.writer;

/**
 * How a language lays its text out: what stands between two words of a line, what one level of a block is indented
 * by, what opens and closes a block, and which quote a text prefers. The words themselves are each node's; the layout
 * is the language's, said once.
 *
 * @param wordSeparator what stands between two words — a blank in SPICE and jMX, nothing in Gerber, whose words are
 *                      written together ({@code X1500Y200D01})
 * @param indentation   one level of a block's indentation
 * @param blockOpening  the word that opens a block, at the end of its line
 * @param blockClosing  the line that closes it
 * @param quote         the quote a text is written in when it holds neither kind
 */
public record SourceLayout(String wordSeparator, String indentation, String blockOpening, String blockClosing, char quote) {

    /** Words a blank apart, blocks in braces indented by four blanks, texts in double quotes. */
    public static final SourceLayout WORDS = new SourceLayout(" ", "    ", "{", "}", '"');

    /** Words written together, as a word-address format writes them: Gerber, Excellon. */
    public static final SourceLayout GLUED = new SourceLayout("", "", "{", "}", '"');

    public SourceLayout withQuote(char preferred) {
        return new SourceLayout(wordSeparator, indentation, blockOpening, blockClosing, preferred);
    }
}
