package org.jmouse.core.context;

/**
 * 🎯 Arguments-oriented context view.
 *
 * <p>
 * Semantic alias over {@link KeyValueContext}
 * for accessing invocation or execution arguments.
 * </p>
 *
 * <p>
 * Does not introduce new storage — only naming clarity.
 * </p>
 */
public interface ArgumentsContext extends KeyValueContext {

    /**
     * 🔍 Get argument by name.
     *
     * @param name argument name
     * @param <T>  expected type
     * @return argument value or {@code null}
     */
    default <T> T getArgument(Object name) {
        return getValue(name);
    }

    /**
     * ❗ Get required argument.
     *
     * @param name argument name
     * @param <T>  expected type
     * @return argument value
     * @throws IllegalStateException if missing
     */
    default <T> T getRequiredArgument(Object name) {
        return getRequiredValue(name);
    }

    /**
     * ❓ Check argument presence.
     *
     * @param name argument name
     * @return {@code true} if present
     */
    default boolean containsArgument(Object name) {
        return containsKey(name);
    }
}
