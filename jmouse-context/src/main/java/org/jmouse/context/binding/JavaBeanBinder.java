package org.jmouse.context.binding;

import org.jmouse.core.reflection.JavaType;
import org.jmouse.core.reflection.TypeDescriptor;
import org.jmouse.util.Priority;

import java.util.function.Supplier;

import static org.jmouse.context.binding.Bindable.of;

@Priority(Integer.MAX_VALUE)
public class JavaBeanBinder extends AbstractBinder {

    public JavaBeanBinder(BindContext context) {
        super(context);
    }

    @Override
    public <T> BindingResult<T> bind(NamePath name, Bindable<T> bindable, DataSource source) {
        TypeDescriptor typeDescriptor = bindable.getTypeDescriptor();
        JavaType       type           = bindable.getType();
        JavaBean<T>    bean           = JavaBean.of(type);
        Supplier<T>    supplier       = bean.getSupplier(bindable);

        if (typeDescriptor.isObject() || typeDescriptor.isScalar()) {
            return bindValue(name, bindable, source);
        }

        for (JavaBean.Property property : bean.getProperties()) {
            JavaType         propertyType = property.getType();
            Supplier<Object> value        = property.getValue(supplier);
            NamePath         propertyName = NamePath.of(property.getName());

            BindingResult<Object> result = bindValue(
                    name.append(propertyName), of(propertyType).withInstance(value), source);

            if (result.isEmpty()) {
                continue;
            }

            if (property.isWritable()) {
                property.setValue(supplier, result.getValue());
            }
        }

        return BindingResult.of(supplier.get());
    }

    @Override
    public <T> boolean supports(Bindable<T> bindable) {
        // fallback binder always true
        return true;
    }

}
