package org.jmouse.context.binding;

import org.jmouse.core.convert.Conversion;
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

        TypeDescriptor valueDescriptor = TypeDescriptor.forClass(value.getType());

        if (typeDescriptor.isScalar() || typeDescriptor.isObject()){
            Object result = value.getRaw();

            if (valueDescriptor.isScalar()) {
                result = convert(result, typeDescriptor);
            }

            return BindingResult.of((T) result);
        } else if (context.isDeepBinding()) {
            return binder.bind(name, bindable, source);
        }

        return BindingResult.empty();
    }

    protected Object convert(Object value, TypeDescriptor typeDescriptor) {
        Conversion conversion = context.getConversion();

        if (conversion != null) {
            value = conversion.convert(value, typeDescriptor.getRawType());
        }

        return value;
    }

}
