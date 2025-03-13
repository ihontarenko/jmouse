package org.jmouse.template.context;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * 🔗 A basic implementation of {@link ScopedChain}.
 * This class manages a <b>stack-based chain of scopes</b>, allowing
 * for hierarchical variable storage and retrieval.
 * <p>
 * Each time a new scope is pushed, it becomes the <b>active scope</b>,
 * and lookups will first check the most recent scope before moving up the chain.
 * <p>
 * 🚀 This class ensures <b>proper scope isolation</b> while allowing
 * for lookups to propagate through the scope hierarchy.
 * <p>
 * Example usage:
 * <pre>{@code
 *     BasicValuesChain chain = new BasicValuesChain();
 *     chain.setValue("x", 42);
 *     chain.push(); // New scope
 *     chain.setValue("x", 100);
 *     chain.getValue("x"); // Returns 100
 *     chain.pop(); // Removes the top scope
 *     chain.getValue("x"); // Returns 42
 * }
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class BasicValuesChain implements ScopedChain {

    /** 🏗️ A stack-based chain of scopes, where the top-most scope is the active one. */
    private final Deque<ScopeValues> chain = new ArrayDeque<>();

    /**
     * 🆕 Creates a new `BasicValuesChain` with an initial core scope.
     */
    public BasicValuesChain() {
        chain.push(new BasicScopeValues());
    }

    /**
     * ⬆️ Removes the current (top-most) scope from the chain and returns it.
     *
     * @return 🔄 the removed scope
     */
    @Override
    public ScopeValues pop() {
        return chain.pop();
    }

    /**
     * ⬇️ Creates and pushes a new **empty** scope onto the chain.
     *
     * @return 🆕 the newly created scope
     */
    @Override
    public ScopeValues push() {
        ScopeValues values = new BasicScopeValues();
        chain.push(values);
        return values;
    }

    /**
     * 🔍 Retrieves the current (top-most) scope without removing it.
     *
     * @return 🎯 the current active scope
     */
    @Override
    public ScopeValues peek() {
        return chain.peek();
    }

    /**
     * 🔗 Provides access to the entire scope chain.
     *
     * @return 📜 an iterable collection of all scopes in the stack
     */
    @Override
    public Iterable<ScopeValues> chain() {
        return chain;
    }
}
