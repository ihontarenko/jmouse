package org.jmouse.formula.el;

import org.jmouse.el.lexer.Token;
import org.jmouse.el.node.Expression;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/** One statement of a formula document, read by {@link FormulaDocumentParser}. */
sealed interface FormulaStatement {

    /** Where the statement starts in the source, for a refusal to point at. */
    int offset();

    /** Where the statement ends in the source: at the {@code ;}, the line break or the end of text that closes it. */
    int end();

    /** The ranges written after the statement — {@code s in 0 .. 2 pi}, {@code k in 1 .. N integer} —, in order. */
    List<Range> ranges();

    /**
     * {@code left = right} at the top level: a definition when another statement follows it, the document's equation
     * when it is the last.
     *
     * @param head  the name and parameters, when the left side is shaped as a definition's: {@code r},
     *              {@code f(u, v)}
     * @param left  the left side, as an expression and as written
     * @param right the right side, as an expression and as written
     */
    record Assignment(Optional<Head> head, Side left, Side right, List<Range> ranges, int offset, int end)
            implements FormulaStatement {
        public Assignment {
            ranges = List.copyOf(ranges);
        }
    }

    /** An expression with no top-level {@code =}: only the last statement may be one. */
    record Plain(Side expression, List<Range> ranges, int offset, int end) implements FormulaStatement {
        public Plain {
            ranges = List.copyOf(ranges);
        }
    }

    /** {@code name} or {@code name(parameter, …)}: what a definition defines. */
    record Head(String name, List<String> parameters) {
    }

    /**
     * A side of a statement: what jME parsed, the source it was cut from, the names written in it, and its tokens — where
     * each of its parts stands in the source.
     */
    record Side(Expression expression, String text, Set<String> names, List<Token> tokens) {
        public Side {
            tokens = List.copyOf(tokens);
        }
    }

    /**
     * {@code name in from .. to}, and {@code integer} after it for a whole index: the span a variable runs over, written
     * after a statement — a curve's parameter, a family's index.
     */
    record Range(String variable, Side from, Side to, boolean integer, int offset) {
    }
}
