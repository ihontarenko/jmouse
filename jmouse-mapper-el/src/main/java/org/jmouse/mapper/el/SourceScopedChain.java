package org.jmouse.mapper.el;

import org.jmouse.el.evaluation.BasicScopeValues;
import org.jmouse.el.evaluation.EvaluationException;
import org.jmouse.el.evaluation.ScopeValues;
import org.jmouse.el.evaluation.ScopedChain;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * A scope chain whose outermost scope is the object being mapped. 🧅
 *
 * <p>Identical to the expression language's own chain but for what sits at the bottom of it: instead of
 * an empty map somebody has to fill, the object itself, read a name at a time. See {@link SourceValues}
 * for why that is not a detail.</p>
 *
 * <h2>⚠️ A variable is set in the CURRENT scope, which is what the contract says</h2>
 *
 * <p>{@link ScopedChain#setValue(String, String)}'s default walks the chain looking for a scope that
 * already holds the name and writes there — while its own documentation says it sets the variable "in
 * the current (top-most) scope". The two disagree, and here the disagreement has teeth: a lambda
 * parameter that happens to share a name with a source property would be written into the source's scope
 * and outlive the lambda, because the scope pushed for it is popped and the value is not in it.</p>
 *
 * <p>So this writes where the documentation says it writes. Nothing in a mapping relies on the walking
 * behaviour — a binding may not shadow a source property, and the language refuses a file where it
 * does.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
final class SourceScopedChain implements ScopedChain {

    private final Deque<ScopeValues> chain = new ArrayDeque<>();

    SourceScopedChain(ScopeValues root) {
        chain.push(root);
    }

    @Override
    public ScopeValues pop() {
        if (chain.size() == 1) {
            throw new EvaluationException("Unable to remove sourceRoot scope values");
        }

        return chain.pop();
    }

    @Override
    public ScopeValues push() {
        ScopeValues values = new BasicScopeValues();

        chain.push(values);

        return values;
    }

    @Override
    public ScopeValues peek() {
        return chain.peek();
    }

    @Override
    public Iterable<ScopeValues> chain() {
        return chain;
    }

    /**
     * {@inheritDoc}
     *
     * <p>⚠️ The current scope, never a scope further out that happens to know the name — see the note on
     * this class.</p>
     */
    @Override
    public void setValue(String name, Object value) {
        peek().set(name, value);
    }
}
