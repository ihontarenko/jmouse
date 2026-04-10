package org.jmouse.jdbc.parameters;

import java.util.List;

import static org.jmouse.core.Verify.argument;

/**
 * Represents a prepared SQL parameterization plan.
 *
 * <p>Contains the original SQL, the compiled SQL ready for execution,
 * and binding metadata describing how parameters are mapped.</p>
 *
 * @param original the original SQL text before compilation
 * @param compiled the compiled SQL text with resolved placeholders
 * @param bindings the ordered list of parameter bindings
 */
public record SQLPlan(String original, String compiled, List<Binding> bindings) {

    /**
     * Returns the total number of parameter bindings in this plan.
     *
     * @return the number of bound parameters
     */
    public int parameterCount() {
        return bindings.size();
    }

    /**
     * Marker contract for a parameter binding declared in the SQL plan.
     *
     * <p>A binding may be positional or named.</p>
     */
    public sealed interface Binding permits Binding.Named, Binding.Positional {

        /**
         * Positional parameter binding.
         *
         * <p>Represents a parameter identified only by its 1-based position.</p>
         *
         * @param position the 1-based parameter index
         * @param kind the binding kind, always {@link Kind#POSITIONAL}
         */
        record Positional(int position, Kind kind) implements Binding {

            /**
             * Creates a positional binding.
             *
             * @param position the 1-based parameter index
             * @param kind the binding kind
             */
            public Positional {
                argument(position > 0, "position must be > 0");
                argument(kind == Kind.POSITIONAL, "invalid kind");
            }

            /**
             * Creates a positional binding with {@link Kind#POSITIONAL}.
             *
             * @param position the 1-based parameter index
             */
            public Positional(int position) {
                this(position, Kind.POSITIONAL);
            }
        }

        /**
         * Named parameter binding.
         *
         * <p>Stores the logical parameter name and the raw token found in the SQL text.</p>
         *
         * @param name the logical binding name
         * @param rawToken the raw token as it appears in SQL
         * @param kind the binding kind, always {@link Kind#NAMED}
         */
        record Named(String name, String rawToken, Kind kind) implements Binding {

            /**
             * Creates a named binding.
             *
             * @param name the logical binding name
             * @param rawToken the raw token as it appears in SQL
             * @param kind the binding kind
             */
            public Named {
                argument(name != null && !name.isBlank(), "name must not be blank");
                argument(rawToken != null && !rawToken.isBlank(), "raw-token must not be blank");
                argument(kind == Kind.NAMED, "invalid kind for named binding");
            }

            /**
             * Creates a named binding with {@link Kind#NAMED}.
             *
             * @param name the logical binding name
             * @param rawToken the raw token as it appears in SQL
             */
            public Named(String name, String rawToken) {
                this(name, rawToken, Kind.NAMED);
            }
        }

        /**
         * Declares supported binding kinds.
         */
        enum Kind {
            NAMED,
            POSITIONAL
        }

        /**
         * Returns the binding kind.
         *
         * @return the binding kind
         */
        Kind kind();
    }
}