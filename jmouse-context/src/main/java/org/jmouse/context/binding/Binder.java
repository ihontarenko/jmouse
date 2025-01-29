package org.jmouse.context.binding;

import org.jmouse.core.reflection.JavaType;

public class Binder extends AbstractBinder {

    private final DataSource    source;
    private final BinderFactory factory;

    public Binder(DataSource source, BinderFactory factory) {
        this.source = source;
        this.factory = factory;
    }

    public Binder(DataSource source) {
        this.source = source;
        this.factory = new DefaultBinderFactory();
    }

    @Override
    public <T> BindingResult<T> bind(String path, Bindable<T> bindable) {
        return bind(PropertyName.of(path), bindable, source);
    }

    @Override
    public <T> BindingResult<T> bind(PropertyName path, Bindable<T> bindable, DataSource source) {
        JavaType     type   = bindable.getType();
        ObjectBinder binder = factory.getBinder(type);
        return binder.bind(path, bindable, source);
    }
}
