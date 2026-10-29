package org.jmouse.el.grammar;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.AbstractExpression;
import org.jmouse.el.writer.SourceWriter;

/** A line kept whole, as the author wrote it: data a block holds without a grammar of its own. */
public class WrittenLineNode extends AbstractExpression {

    private final String written;

    public WrittenLineNode(String written) {
        this.written = written;
    }

    public String written() {
        return written;
    }

    @Override
    public Object evaluate(EvaluationContext context) {
        return written;
    }

    @Override
    public void writeSource(SourceWriter writer) {
        writer.word(written);
    }
}
