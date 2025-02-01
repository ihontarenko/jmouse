package org.jmouse.context.binding;

import org.jmouse.core.reflection.JavaType;
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
        JavaType       type           = bindable.getType();

        if (value.isNull()) {
            return BindingResult.empty();
        }

        TypeDescriptor valueDescriptor = TypeDescriptor.forClass(value.getType());

        if (typeDescriptor.isScalar() || typeDescriptor.isObject()){
            Object result = value.getRaw();

            if (!value.isNull() && valueDescriptor.isScalar() && !valueDescriptor.is(typeDescriptor.getRawType())) {
                result = context.getConversion().convert(result, type.getRawType());
            }

            return BindingResult.of((T) result);
        } else if (context.isDeepBinding()) {
            return binder.bind(name, bindable, source);
        }

        return BindingResult.empty();
    }
}
