package org.jmouse.parallax.script.reader;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.AbstractExpression;

/** A statement of a document's braces in the parse tree. */
public final class DocumentStatementNode extends AbstractExpression {

    private final DocumentStatement statement;

    DocumentStatementNode(DocumentStatement statement) {
        this.statement = statement;
    }

    DocumentStatement statement() {
        return statement;
    }

    @Override
    public Object evaluate(EvaluationContext context) {
        return this;
    }

    @Override
    public String toString() {
        return statement.toString();
    }
}
