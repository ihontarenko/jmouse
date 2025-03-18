package org.jmouse.el.evaluation;

/**
 * 🔗 Represents a scoped chain of variable contexts.
 * This interface allows managing <b>nested variable scopes</b>, enabling
 * hierarchical lookups and modifications of values.
 * <p>
 * The <b>most local scope</b> is searched first when retrieving values, then
 * the search continues upwards through the chain.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public interface ScopedChain {

    /**
     * ⬆️ Removes the current scope from the chain and returns it.
     *
     * @return 🔄 the removed scope
     */
    ScopeValues pop();

    /**
     * ⬇️ Pushes a new empty scope onto the chain.
     *
     * @return 🆕 the newly created scope
     */
    ScopeValues push();

    /**
     * 🔍 Retrieves the current (top-most) scope without removing it.
     *
     * @return 🎯 the current scope
     */
    ScopeValues peek();

    /**
     * 🔗 Provides access to the entire chain of scopes.
     *
     * @return 📜 an iterable collection of all scopes
     */
    Iterable<ScopeValues> chain();

    /**
     * 📌 Retrieves the value associated with the given variable name.
     * The lookup starts from the **most local scope** and moves **upward**
     * through the chain until the variable is found.
     *
     * @param name 🏷️ the variable name
     * @return 🔢 the variable value, or {@code null} if not found
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

    /**
     * ✏️ Sets the value of a variable in the <b>current (top-most) scope</b>.
     *
     * @param name 🏷️ the variable name
     * @param value 🔢 the new value to set
     */
    default void setValue(String name, Object value) {
        peek().set(name, value);
    }

    /**
     * ✅ Checks if a variable exists in the <b>current (top-most) scope</b>.
     *
     * @param name 🏷️ the variable name
     * @return 🔍 {@code true} if the variable exists, otherwise {@code false}
     */
    default boolean contains(String name) {
        return peek().contains(name);
    }
}
