package org.jmouse.access.el.node;

import org.jmouse.access.el.SourceWriter;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.AbstractExpression;

import java.util.ArrayList;
import java.util.List;

/**
 * One {@code paid a, b} line — capabilities that are closed until something grants them.
 *
 * <h2>⚠️ Why this is a line and not a kind</h2>
 *
 * <p>Being paid and having a shape are independent facts: {@code seat} is a limit <em>and</em> closed
 * until a bundle grants it. Folding the two into one word would make {@code paid} and {@code limit}
 * mutually exclusive, which they are not, and the file would have no way left to say the common case.
 *
 * <h2>⚠️ Why this is not a nested block</h2>
 *
 * <p>{@code paid { … }} would need a block parser, a block node and its own rejection message to say
 * one thing: a list of keys. A repeatable line says the same thing with no nesting, and a product that
 * sells ten things writes ten lines rather than one long one.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class PaidCapabilitiesNode extends AbstractExpression {

    private List<String> keys = new ArrayList<>();

    public List<String> getKeys() {
        return keys;
    }

    public void setKeys(List<String> keys) {
        this.keys = keys == null ? new ArrayList<>() : keys;
    }

    @Override
    public Object evaluate(EvaluationContext context) {
        return List.copyOf(keys);
    }

    @Override
    public String toSource() {
        return "paid " + String.join(", ", keys.stream().map(SourceWriter::name).toList());
    }

    @Override
    public String toString() {
        return toSource();
    }
}
