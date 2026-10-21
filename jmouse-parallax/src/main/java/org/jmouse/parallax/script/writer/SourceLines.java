package org.jmouse.parallax.script.writer;

import org.jmouse.parallax.script.engine.Notation;

import java.util.function.Consumer;

/**
 * The lines of a jMX text as they are written: each at the depth of the braces around it, four spaces a level, a
 * block's lines one level deeper than its opening.
 */
final class SourceLines {

    private static final String LEVEL = "    ";

    private final StringBuilder text = new StringBuilder();
    private int depth;

    /** The indentation of the line about to be written: what a text spanning lines continues at. */
    String indentation() {
        return LEVEL.repeat(depth);
    }

    /** One line at the current depth. A value spanning lines — a long text — carries its own indentation after the first. */
    void line(String line) {
        text.append(indentation()).append(line).append('\n');
    }

    /** {@code opening { … }}: the lines {@code body} writes, one level deeper. */
    void block(String opening, Consumer<SourceLines> body) {
        line(opening + " {");
        depth++;
        body.accept(this);
        depth--;
        line("}");
    }

    /**
     * {@code opening { … }} around a body another language reads — jMF definitions, jMS —, its lines as written, their
     * own indentation kept relative to each other.
     *
     * @param written the text between the braces, as the reader cut it out
     */
    void verbatim(String opening, String written) {
        line(opening + " {");
        String inner = LEVEL.repeat(depth + 1);
        if (!written.isBlank()) {
            for (String physical : Notation.bodyWithoutIndentation(written).split("\n", -1)) {
                text.append(physical.isBlank() ? "" : inner + physical).append('\n');
            }
        }
        line("}");
    }

    String text() {
        return text.toString();
    }
}
