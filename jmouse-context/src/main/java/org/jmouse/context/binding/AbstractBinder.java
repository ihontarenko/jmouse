package org.jmouse.context.binding;

import org.jmouse.core.reflection.JavaType;
import org.jmouse.core.reflection.Reflections;

abstract public class AbstractBinder implements ObjectBinder {

    @Override
    public <T> BindingResult<T> bind(String name, Bindable<T> bindable) {
        throw new UnsupportedOperationException(
                "Unsupported method call for this '%s' class"
                        .formatted(Reflections.getShortName(this)));
    }

    @Override
    public boolean supports(JavaType type) {
        return false;
    }

}
