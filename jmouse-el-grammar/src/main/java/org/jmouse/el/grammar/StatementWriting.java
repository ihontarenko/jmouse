package org.jmouse.el.grammar;

import org.jmouse.el.node.Expression;
import org.jmouse.el.node.Node;
import org.jmouse.el.node.Trivia;
import org.jmouse.el.writer.SourceWriter;

/**
 * How a statement of a document is written back with what its author wrote around it: the comments and blank lines the
 * engine's statement reader kept above it, the line itself, the comment it carries at its end. A body that writes its
 * statements through here loses nothing a person wrote.
 */
public final class StatementWriting {

    private StatementWriting() {
    }

    /** The statements of a body, one line each, every one with its trivia. */
    public static void statements(SourceWriter writer, Iterable<? extends Expression> statements) {
        for (Expression statement : statements) {
            statement(writer, statement);
        }
    }

    /** One statement: what stands above it, the statement, what stands beside it, the end of its line. */
    public static void statement(SourceWriter writer, Expression statement) {
        leading(writer, statement);
        statement.writeSource(writer);
        Trivia trailing = statement.getTrailingTrivia();
        if (trailing != null) {
            writer.word(trailing.text());
        }
        writer.endLine();
    }

    /** The comments and blank lines written above a node. */
    public static void leading(SourceWriter writer, Node node) {
        for (Trivia trivia : node.getLeadingTrivia()) {
            if (trivia.isBlank()) {
                writer.blankLine();
            } else {
                writer.word(trivia.text()).endLine();
            }
        }
    }
}
