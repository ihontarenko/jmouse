package org.jmouse.cad.document;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.AbstractExpression;
import org.jmouse.el.writer.SourceWriter;

import java.util.List;

/**
 * A declaration of a document's body — {@code extends diode}, {@code units A, B}, {@code use cmos-hc, smooth} —: what
 * the document is built on, said before its blocks.
 */
public class DeclarationNode extends AbstractExpression {

    private final String       word;
    private final List<String> names;

    public DeclarationNode(String word, List<String> names) {
        this.word = word;
        this.names = List.copyOf(names);
    }

    public String word() {
        return word;
    }

    public List<String> names() {
        return names;
    }

    @Override
    public Object evaluate(EvaluationContext context) {
        return names;
    }

    @Override
    public void writeSource(SourceWriter writer) {
        writer.word(word);
        for (int index = 0; index < names.size(); index++) {
            if (index > 0) {
                writer.join(",");
            }
            writer.word(names.get(index));
        }
    }
}
