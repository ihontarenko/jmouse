package org.jmouse.script.el.node;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.AbstractExpression;
import org.jmouse.el.node.Expression;
import org.jmouse.el.node.expression.SpanNode;
import org.jmouse.script.el.SourceSpan;

/**
 * Where in the file a construction was written, as a node.
 *
 * <p>⚠️ Not decoration. The binder refuses a document by saying <em>"unknown event {@code unlod}"</em>
 * and it has to name the line, or the file stops being editable. Every node the parser builds carries
 * one of these.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class ScriptSpanNode extends SpanNode {

    /**
     * Constructs a span.
     *
     * @param lineNumber the 1-based line
     * @param column     the 1-based column within that line
     */
    private String document;

    public ScriptSpanNode(int lineNumber, int column) {
        super(lineNumber, column);
    }

    /**
     * Names the file this position is in.
     *
     * <p>⚠️ <strong>Set by a loader, never by the parser.</strong> A parser reads one text and has no
     * opinion about where it came from; the loader is what knows, and it has to say so <em>before</em> a
     * merge, because after several files become one document "line 12" belongs to nothing in
     * particular. That is the failure this method exists to prevent, and it is invisible until a
     * document has an {@code include} in it.</p>
     *
     * @param document what the file is called
     */
    public void setDocument(String document) {
        this.document = document;
    }

    /**
     * Returns this position as the record a failure is reported with.
     *
     * @return the file, the line and the column
     */
    public SourceSpan toSourceSpan() {
        return new SourceSpan(document, lineNumber(), position());
    }

    /**
     * Returns where an expression was written, or {@link SourceSpan#none()} when it carries no span.
     *
     * <p>Reporting a failure should never itself be the reason a load dies, so a missing span degrades
     * to "nowhere" rather than throwing on the way to explaining a different problem.</p>
     *
     * @param expression the expression to locate
     * @return its position in the file
     */
    public static SourceSpan at(Expression expression) {
        if (expression instanceof AbstractExpression node && node.getSpan() instanceof ScriptSpanNode span) {
            return span.toSourceSpan();
        }

        return SourceSpan.none();
    }

    @Override
    public Object evaluate(EvaluationContext context) {
        return toSourceSpan();
    }

}
