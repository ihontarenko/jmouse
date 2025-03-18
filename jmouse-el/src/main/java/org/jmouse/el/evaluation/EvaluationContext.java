package org.jmouse.el.evaluation;

import org.jmouse.core.bind.Bind;
import org.jmouse.core.bind.PropertyPath;
import org.jmouse.core.bind.ObjectAccessor;
import org.jmouse.core.convert.Conversion;
import org.jmouse.el.extension.ExtensionContainer;

public interface EvaluationContext {

    ScopedChain getScopedChain();

    ExtensionContainer getExtensions();

    Conversion getConversion();

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

    default void setValue(String name, Object value) {
        if (name.contains(".") || name.contains("[")) {
            ObjectAccessor accessor = getValueAccessor().navigate(PropertyPath.forPath(name).sup(1));
            // todo: try use binder

            Bind.with(getValueAccessor()).to(name, getValueAccessor().unwrap());

            System.out.println(accessor);
        } else {
            getScopedChain().setValue(name, value);
        }
    }

    default ObjectAccessor getValueAccessor() {
        return new ScopedChainValuesAccessor(getScopedChain());
    }

}
