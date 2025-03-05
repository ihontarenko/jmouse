package org.jmouse.template.evaluation;

public interface ScopedChain {

    ScopeValues pop();

    ScopeValues push();

    ScopeValues peek();

    Iterable<ScopeValues> chain();

    /**
     * Retrieves the value associated with the given variable name.
     * The lookup searches from the most local scope upward.
     *
     * @param name the variable name
     * @return the variable value, or null if not found
     */
    default Object getValue(String name) {
        Object value = peek().get(name);

        if (value == null) {
            for (ScopeValues values : chain()) {
                if (values.contains(name)) {
                    value = values.get(name);
                    break;
                }
            }
        }

        return value;
    }

    default void setValue(String name, Object value) {
        peek().set(name, value);
    }

    default boolean contains(String name) {
        return peek().contains(name);
    }

}
