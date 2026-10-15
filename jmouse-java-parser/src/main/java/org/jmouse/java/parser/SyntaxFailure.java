package org.jmouse.java.parser;

import org.jmouse.java.parser.source.ParseProblem;
import org.jmouse.java.parser.source.ProblemKind;
import org.jmouse.java.parser.source.SourceSpan;

/**
 * Unwinds the parser to the nearest recovery point. It is control flow, never seen by callers: every
 * instance becomes a {@link ParseProblem} where it is caught, so it carries no stack trace.
 */
final class SyntaxFailure extends RuntimeException {

    private final transient ParseProblem problem;

    SyntaxFailure(ProblemKind kind, String message, SourceSpan span) {
        super(message, null, false, false);
        this.problem = new ParseProblem(kind, message, span);
    }

    ParseProblem problem() {
        return problem;
    }
}
