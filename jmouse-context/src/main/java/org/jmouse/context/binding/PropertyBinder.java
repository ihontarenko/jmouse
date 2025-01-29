package org.jmouse.context.binding;

@FunctionalInterface
public interface PropertyBinder {

    <T> BindingResult<T> bind(String name, Bindable<T> bindable);

}