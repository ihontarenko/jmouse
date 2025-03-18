package org.jmouse.el.evaluation;

public class DefaultEvaluationContext implements EvaluationContext {

    private final ScopedChain chain;

    public DefaultEvaluationContext(ScopedChain chain) {
        this.chain = chain;
    }

    public DefaultEvaluationContext() {
        this(new BasicValuesChain());
    }

    @Override
    public ScopedChain getScopedChain() {
        return chain;
    }

}
