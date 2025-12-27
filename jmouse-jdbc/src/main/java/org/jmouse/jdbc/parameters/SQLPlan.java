package org.jmouse.jdbc.parameters;

import org.jmouse.core.Contract;
import org.jmouse.el.node.Expression;

import java.util.List;

public record SQLPlan(String original, String compiled, List<Binding> bindings) {

    public int parameterCount() {
        return bindings.size();
    }

    public sealed interface Binding permits Binding.Named, Binding.Positional {
        record Positional(int position, Kind kind) implements Binding {
            public Positional {
                Contract.argument(position > 0, "position must be > 0");
                Contract.argument(kind == Kind.POSITIONAL, "invalid kind");
            }
            public Positional(int position) {
                this(position, Kind.POSITIONAL);
            }
        }
        record Named(String name, String rawToken, Expression expression, Kind kind) implements Binding {
            public Named {
                Contract.argument(name != null && !name.isBlank(), "name must not be blank");
                Contract.argument(rawToken != null && !rawToken.isBlank(), "raw-token must not be blank");
                Contract.argument(expression != null, "expression must not be null");
                Contract.argument(kind == Kind.NAMED, "invalid kind for named binding");
            }
            public Named(String name, String rawToken, Expression expression) {
                this(name, rawToken, expression, Kind.NAMED);
            }
        }
        enum Kind { NAMED, POSITIONAL }
        Kind kind();
    }
}