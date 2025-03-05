package org.jmouse.template.evaluation;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

public class BasicValuesChain implements ScopedChain {

    private final Deque<ScopeValues> chain = new ArrayDeque<>();

    public BasicValuesChain() {
        chain.push(new BasicScopeValues());
    }

    @Override
    public ScopeValues pop() {
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
}
