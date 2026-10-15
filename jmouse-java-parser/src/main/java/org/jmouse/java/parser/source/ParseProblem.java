package org.jmouse.java.parser.source;

/**
 * Something in the source that was not read as written, and where it is.
 *
 * <p>A problem never stops parsing: the declaration it occurred in is skipped or kept partially, and
 * everything around it is still produced. The message names tokens and positions only, never source
 * text beyond the offending token.
 *
 * @param kind    the category
 * @param message a short English explanation
 * @param span    where it occurred
 */
public record ParseProblem(ProblemKind kind, String message, SourceSpan span) {

    /**
     * Validates the components.
     */
    public ParseProblem {
        if (kind == null || message == null || message.isBlank() || span == null) {
            throw new IllegalArgumentException("A parse problem needs a kind, a message and a span.");
        }
    }

    @Override
    public String toString() {
        return kind + " at " + span.start() + ": " + message;
    }
}
