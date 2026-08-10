package org.jmouse.access.el.node;

import org.jmouse.access.policy.model.SourceSpan;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.AbstractExpression;
import org.jmouse.el.node.Expression;
import org.jmouse.el.node.expression.SpanNode;

/**
 * Where in the file a construction was written, as a node.
 *
 * <p>⚠️ Not decoration. Stage 2 refuses a document by saying <em>"unknown scope {@code SPCE}"</em>
 * and it has to name the line, or the file stops being editable. Every node the parser builds
 * carries one of these.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class SourceSpanNode extends SpanNode {

    /**
     * Constructs a span.
     *
     * @param lineNumber the 1-based line
     * @param column     the 1-based column within that line
     */
    public SourceSpanNode(int lineNumber, int column) {
        super(lineNumber, column);
    }

    /**
     * Returns this position as the record stage 2 receives.
     *
     * @return the line and column
     */
    public SourceSpan toSourceSpan() {
        return new SourceSpan(lineNumber(), position());
    }

    /**
     * Returns where an expression was written, or {@link SourceSpan#none()} when it carries no span.
     *
     * <p>Reported failures should never be the reason a parse dies, so a missing span degrades to
     * "nowhere" rather than throwing on the way to explaining a different problem.</p>
     *
     * @param expression the expression to locate
     * @return its position in the file
     */
    public static SourceSpan at(Expression expression) {
        if (expression instanceof AbstractExpression node && node.getSpan() instanceof SourceSpanNode span) {
            return span.toSourceSpan();
        }

        return SourceSpan.none();
    }

    @Override
    public Object evaluate(EvaluationContext context) {
        return toSourceSpan();
    }

}
