package org.jmouse.context.binding;

import org.jmouse.core.reflection.TypeDescriptor;

abstract public class AbstractBinder implements ObjectBinder {

    protected final BindContext context;

    public AbstractBinder(BindContext context) {
        this.context = context;
    }

    @Override
    public <T> BindingResult<T> bindValue(NamePath name, Bindable<T> bindable, DataSource source) {
        TypeDescriptor typeDescriptor = bindable.getTypeDescriptor();
        DataSource     value          = source.get(name);
        ObjectBinder   binder         = context.getRootBinder();

        if (value.isNull()) {
            return BindingResult.empty();
        }

        if (typeDescriptor.isScalar() || bindable.getType().getRawType() == Object.class){
            // todo: add conversion and post processing
            Object result = value.getRaw();
            return BindingResult.of((T) result);
        } else if (context.isDeepBinding()) {
            return binder.bind(name, bindable, source);
        }

        return BindingResult.empty();
    }
}
