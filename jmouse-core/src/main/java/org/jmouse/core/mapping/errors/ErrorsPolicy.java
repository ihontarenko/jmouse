package org.jmouse.core.mapping.errors;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Error handling policy by error code (exact) and/or prefix.
 */
public final class ErrorsPolicy {

    private final Map<String, ErrorAction> prefixes;
    private final Map<String, ErrorAction> exact;
    private final ErrorAction              defaultAction;

    private ErrorsPolicy(Builder builder) {
        this.exact = Map.copyOf(builder.exact);
        this.prefixes = Map.copyOf(builder.prefixes);
        this.defaultAction = builder.defaultAction;
    }

    public static Builder builder() {
        return new Builder();
    }

    public ErrorAction getActionFor(String code) {
        if (code == null) {
            return defaultAction;
        }

        ErrorAction action = exact.get(code);

        if (action != null) {
            return action;
        }

        for (Map.Entry<String, ErrorAction> entry : prefixes.entrySet()) {
            if (code.startsWith(entry.getKey())) {
                return entry.getValue();
            }
        }

        return defaultAction;
    }

    public static final class Builder {

        private final Map<String, ErrorAction> prefixes      = new LinkedHashMap<>();
        private final Map<String, ErrorAction> exact         = new LinkedHashMap<>();
        private       ErrorAction              defaultAction = ErrorAction.THROW;

        public Builder onCode(String code, ErrorAction action) {
            exact.put(Objects.requireNonNull(code), Objects.requireNonNull(action));
            return this;
        }

        public Builder onPrefix(String prefix, ErrorAction action) {
            prefixes.put(Objects.requireNonNull(prefix), Objects.requireNonNull(action));
            return this;
        }

        public Builder defaultAction(ErrorAction action) {
            this.defaultAction = Objects.requireNonNull(action);
            return this;
        }

        public ErrorsPolicy build() {
            return new ErrorsPolicy(this);
        }
    }
}
