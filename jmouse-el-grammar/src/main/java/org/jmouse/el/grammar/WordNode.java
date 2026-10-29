package org.jmouse.el.grammar;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.AbstractExpression;
import org.jmouse.el.writer.SourceWriter;

/** One word of a line, hyphens and all — {@code through-hole}, {@code R1} —, or a text in quotes when {@code quoted}. */
public class WordNode extends AbstractExpression {

    private final String  word;
    private final boolean quoted;

    public WordNode(String word, boolean quoted) {
        this.word = word;
        this.quoted = quoted;
    }

    public String word() {
        return word;
    }

    @Override
    public Object evaluate(EvaluationContext context) {
        return word;
    }

    @Override
    public void writeSource(SourceWriter writer) {
        if (quoted) {
            writer.text(word);
        } else {
            writer.word(word);
        }
    }
}
