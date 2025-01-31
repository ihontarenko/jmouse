package org.jmouse.context.binding;

public interface ObjectBinder {

    <T> BindingResult<T> bind(NamePath name, Bindable<T> bindable, DataSource source);

    <T> BindingResult<T> bindValue(NamePath name, Bindable<T> bindable, DataSource source);

    default <T> boolean supports(Bindable<T> bindable) {
        return false;
    }

}
