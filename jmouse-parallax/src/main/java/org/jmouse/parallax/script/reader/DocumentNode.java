package org.jmouse.parallax.script.reader;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.AbstractExpression;
import org.jmouse.parallax.script.ExperienceDocument;

/** A whole document in the parse tree: {@code component "1N4148" as 1n4148 { … }}, read and checked. */
public final class DocumentNode extends AbstractExpression {

    private final ExperienceDocument document;

    DocumentNode(ExperienceDocument document) {
        this.document = document;
    }

    public ExperienceDocument document() {
        return document;
    }

    @Override
    public Object evaluate(EvaluationContext context) {
        return document;
    }

    @Override
    public String toString() {
        return document.kind().keyword() + " \"" + document.title() + "\"";
    }
}
