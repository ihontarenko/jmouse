package org.jmouse.context.binding;

import org.jmouse.core.reflection.JavaType;

public interface ObjectBinder extends PropertyBinder {

    <T> BindingResult<T> bind(PropertyName name, Bindable<T> bindable, DataSource source);

    default boolean supports(JavaType type) {
        return false;
    }

}
