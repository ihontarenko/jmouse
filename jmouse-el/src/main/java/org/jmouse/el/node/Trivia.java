package org.jmouse.el.node;

/**
 * A comment, or a blank line — what a person wrote that the grammar does not read. 🗒️
 *
 * <h2>⚠️ It is not noise, and calling it "whitespace" is how it gets thrown away</h2>
 *
 * <p>A parser is entitled to ignore trivia; a <strong>writer is not</strong>. Any language on this
 * engine that can render a tree back into source can also be edited through a builder, and a builder
 * that opens a file, saves it, and silently drops every explanation somebody wrote above their rules is
 * a builder nobody saves from twice.</p>
 *
 * <h2>⚠️ A blank line is trivia too</h2>
 *
 * <p>Comments alone are not enough. A document whose paragraphs collapse into one block round-trips
 * without losing a single character and is unrecognisable to whoever wrote it — the grouping was the
 * point, and it lived entirely in the empty lines.</p>
 *
 * @param kind whether it is something written or something left out
 * @param text the comment exactly as typed, {@code #} included; empty for a blank line
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public record Trivia(Kind kind, String text) {

    /** What a piece of trivia is. */
    public enum Kind {

        /** A {@code #} line, or the tail of a line after one. */
        COMMENT,

        /** One empty line, kept because grouping is meaning. */
        BLANK
    }

    /**
     * A comment.
     *
     * @param text the line as typed, {@code #} included
     * @return the trivia
     */
    public static Trivia comment(String text) {
        return new Trivia(Kind.COMMENT, text);
    }

    /** @return one blank line */
    public static Trivia blank() {
        return new Trivia(Kind.BLANK, "");
    }

    /** @return whether this is a blank line */
    public boolean isBlank() {
        return kind == Kind.BLANK;
    }

    @Override
    public String toString() {
        return isBlank() ? "<blank>" : text;
    }
}
