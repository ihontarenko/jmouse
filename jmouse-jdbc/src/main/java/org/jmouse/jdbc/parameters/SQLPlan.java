package org.jmouse.jdbc.parameters;

import org.jmouse.el.node.Expression;

import java.util.List;

public record SQLPlan(String original, String compiled, List<Binding> bindings) {

    public int parameterCount() {
        return bindings.size();
    }

    public sealed interface Binding permits Binding.Named, Binding.Positional {
        record Named(String name, String rawToken, Expression expression) implements Binding { }
        record Positional(int position) implements Binding { }
    }
}