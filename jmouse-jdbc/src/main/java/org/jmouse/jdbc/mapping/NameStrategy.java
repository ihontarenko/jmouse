package org.jmouse.jdbc.mapping;

/**
 * Strategy interface for transforming column labels or names into
 * target property names.
 * <p>
 * {@code NameStrategy} is typically used during result set mapping
 * (e.g. bean/property binding) to adapt database naming conventions
 * to Java naming conventions.
 *
 * <h3>Common use cases</h3>
 * <ul>
 *     <li>Mapping {@code snake_case} database columns to {@code camelCase} Java properties</li>
 *     <li>Preserving column names as-is</li>
 *     <li>Adapting legacy or mixed naming styles</li>
 * </ul>
 *
 * <h3>Examples</h3>
 * <pre>{@code
 * NameStrategy camel = NameStrategy.toCamel();
 * camel.toName("user_name"); // -> "userName"
 *
 * NameStrategy snake = NameStrategy.toSnake();
 * snake.toName("userName"); // -> "user_name"
 * }</pre>
 *
 * <p>
 * Implementations are expected to be stateless and reusable.
 *
 * @author jMouse
 */
@FunctionalInterface
public interface NameStrategy {

    /**
     * Returns a strategy that leaves column names unchanged.
     *
     * @return identity name strategy
     */
    static NameStrategy asIs() {
        return c -> c;
    }

    /**
     * Returns a strategy that converts column names to {@code camelCase}.
     * <p>
     * The following separators are recognized:
     * <ul>
     *     <li>underscore ({@code _})</li>
     *     <li>dash ({@code -})</li>
     *     <li>space ({@code ' '})</li>
     * </ul>
     *
     * <h3>Example</h3>
     * <pre>{@code
     * NameStrategy.toCamel().toName("user_name");   // userName
     * NameStrategy.toCamel().toName("user-name");  // userName
     * NameStrategy.toCamel().toName("user name");  // userName
     * }</pre>
     *
     * @return camelCase name strategy
     */
    static NameStrategy toCamel() {
        return column -> {
            StringBuilder builder = new StringBuilder(column.length());
            boolean       upper   = false;
            for (int index = 0; index < column.length(); index++) {
                char character = column.charAt(index);
                if (character == '_' || character == '-' || character == ' ') {
                    upper = true;
                    continue;
                }
                builder.append(upper ? Character.toUpperCase(character) : character);
                upper = false;
            }
            return builder.toString();
        };
    }

    /**
     * Returns a strategy that converts {@code camelCase} names to {@code snake_case}.
     *
     * <h3>Example</h3>
     * <pre>{@code
     * NameStrategy.toSnake().toName("userName"); // user_name
     * }</pre>
     *
     * @return snake_case name strategy
     */
    static NameStrategy toSnake() {
        return column -> {
            StringBuilder builder = new StringBuilder(column.length());
            boolean       upper   = false;
            for (int index = 0; index < column.length(); index++) {
                char c = column.charAt(index);
                char u = (index > 0 && index < column.length() - 1 ? '_' : 0);
                if (Character.isUpperCase(c)) {
                    upper = true;
                    continue;
                }
                builder.append(upper ? u + Character.toLowerCase(c) : c);
                upper = false;
            }
            return builder.toString();
        };
    }

    /**
     * Transforms the given column label or name into a target name.
     *
     * @param label original column label or name
     * @return transformed name
     */
    String toName(String label);
}
