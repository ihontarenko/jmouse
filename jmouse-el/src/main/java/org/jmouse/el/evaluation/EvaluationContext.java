package org.jmouse.el.evaluation;

import org.jmouse.core.bind.PropertyValuesAccessor;
import org.jmouse.el.extension.ExtensionContainer;

public interface EvaluationContext {

    ScopedChain getScopedChain();

    ExtensionContainer getExtensions();

    default Object getValue(String name) {
        Object      value = null;
        ScopedChain chain = getScopedChain();

        if (name.contains(".") || name.contains("[")) {
            value = getValueAccessor().navigate(name).asObject();
        }

        if (value == null && chain.contains(name)) {
            value = chain.getValue(name);
        }

        return value;
    }

    default PropertyValuesAccessor getValueAccessor() {
        return new ScopedChainValuesAccessor(getScopedChain());
    }

}
