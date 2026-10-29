package org.jmouse.el.grammar;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.expression.ExpressionsNode;
import org.jmouse.el.writer.SourceWriter;

/**
 * A block of a document — {@code word { line … }} —, its lines as its expressions. A module's block extends it with
 * what its lines mean; it writes itself as its word and its lines in braces, each line writing itself.
 */
public abstract class BlockNode extends ExpressionsNode {

    /** The word the block opens with. */
    public abstract String word();

    @Override
    public Object evaluate(EvaluationContext context) {
        return this;
    }

    @Override
    public void writeSource(SourceWriter writer) {
        writer.word(word());
        writer.block(body -> StatementWriting.statements(body, getExpressions()));
    }

    @Override
    public String toString() {
        return word() + " { " + getExpressions().size() + " lines }";
    }
}
