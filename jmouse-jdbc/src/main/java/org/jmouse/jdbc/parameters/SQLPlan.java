package org.jmouse.jdbc.parameters;

import org.jmouse.core.Verify;
import org.jmouse.el.node.Expression;

import java.util.List;

public record SQLPlan(String original, String compiled, List<Binding> bindings) {

    public int parameterCount() {
        return bindings.size();
    }

    public sealed interface Binding permits Binding.Named, Binding.Positional {
        record Positional(int position, Kind kind) implements Binding {
            public Positional {
                Verify.argument(position > 0, "position must be > 0");
                Verify.argument(kind == Kind.POSITIONAL, "invalid kind");
            }
            public Positional(int position) {
                this(position, Kind.POSITIONAL);
            }
        }
        record Named(String name, String rawToken, Expression expression, Kind kind) implements Binding {
            public Named {
                Verify.argument(name != null && !name.isBlank(), "name must not be blank");
                Verify.argument(rawToken != null && !rawToken.isBlank(), "raw-token must not be blank");
                Verify.argument(expression != null, "expression must not be null");
                Verify.argument(kind == Kind.NAMED, "invalid kind for named binding");
            }
            public Named(String name, String rawToken, Expression expression) {
                this(name, rawToken, expression, Kind.NAMED);
            }
        }
        enum Kind { NAMED, POSITIONAL }
        Kind kind();
    }
}