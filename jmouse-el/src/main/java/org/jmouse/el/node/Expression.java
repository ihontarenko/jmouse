package org.jmouse.el.node;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.writer.SourceWritable;
import org.jmouse.el.writer.SourceWriter;

/**
 * 🧩 Represents an expression node that can be evaluated within an {@link EvaluationContext}.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public interface Expression extends Node, SourceWritable {

    /**
     * 🏗️ Evaluates the expression within the given context.
     *
     * @param context 🎛️ The evaluation context containing variables and functions.
     * @return 📤 The result of evaluating the expression.
     * @throws UnsupportedOperationException if evaluation is not implemented.
     */
    default Object evaluate(EvaluationContext context) {
        throw new UnsupportedOperationException(
                "Evaluation for expression node '%s' is not implemented"
                        .formatted(this.getClass().getName()));
    }

    /**
     * Returns the source representation of this expression.
     *
     * @return the expression as source code
     * @throws UnsupportedOperationException if source rendering is not supported
     */
    default String toSource() {
        throw new UnsupportedOperationException(
                "Source rendering is not supported for expression '%s'"
                        .formatted(getClass().getName()));
    }

    /**
     * Writes this expression into a {@link SourceWriter} — the way a tree is made text. A node of a language on the
     * writer overrides it and says its words; one that only has {@link #toSource()} is written as that text, so the two
     * kinds of node mix in one tree.
     *
     * @param writer what the source is written into
     */
    @Override
    default void writeSource(SourceWriter writer) {
        writer.verbatim(toSource());
    }

}
