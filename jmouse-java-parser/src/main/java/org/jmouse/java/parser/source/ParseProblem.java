package org.jmouse.java.parser.source;

/**
 * Something in the source that was not read as written, and where it is.
 *
 * <p>A problem never stops parsing: the declaration it occurred in is skipped or kept partially,
 * and everything around it is still produced. The message names tokens and positions only, never
 * source text beyond the offending token.
 *
 * @param kind the category
 * @param message a short English explanation
 * @param span where it occurred
 */
public record ParseProblem(ProblemKind kind, String message, SourceSpan span, Phase phase) {

    /**
     * The reader that reported the problem; body recovery does not invalidate declaration headers.
     */
    public enum Phase {
        LEXICAL,
        STRUCTURE,
        BODY
    }

    public ParseProblem(ProblemKind kind, String message, SourceSpan span) {
        this(
                kind,
                message,
                span,
                switch (kind) {
                    case UNTERMINATED_COMMENT,
                                    UNTERMINATED_STRING,
                                    UNTERMINATED_CHARACTER,
                                    UNTERMINATED_TEXT_BLOCK,
                                    MALFORMED_TEXT_BLOCK,
                                    INVALID_UNICODE_ESCAPE,
                                    UNEXPECTED_CHARACTER ->
                            Phase.LEXICAL;
                    default -> Phase.STRUCTURE;
                });
    }

    /** Validates the components. */
    public ParseProblem {
        if (phase == null || kind == null || message == null || message.isBlank() || span == null) {
            throw new IllegalArgumentException(
                    "A parse problem needs a kind, a message and a span.");
        }
    }

    @Override
    public String toString() {
        return kind + " at " + span.start() + ": " + message;
    }
}
