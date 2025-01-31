package org.jmouse.context.binding;

import org.jmouse.core.reflection.JavaType;
import org.jmouse.util.Priority;

import java.util.function.Supplier;

@Priority(Integer.MAX_VALUE)
public class JavaBeanBinder extends AbstractBinder {

    public JavaBeanBinder(BindContext context) {
        super(context);
    }

    @Override
    public <T> BindingResult<T> bind(NamePath name, Bindable<T> bindable, DataSource source) {
        Class<T>    type     = (Class<T>) bindable.getType().getRawType();
        JavaBean<T> bean     = JavaBean.of(type);
        Supplier<T> supplier = bean.getSupplier(bindable);

        for (JavaBean.Property property : bean.getProperties()) {
            JavaType propertyType = property.getType();
            NamePath propertyName = NamePath.of(property.getName());

            System.out.println("propertyName: " + propertyName);

            bindValue(propertyName, Bindable.of(propertyType), source.get(name)).ifPresent((Object v) -> {
                if (property.isWritable()) {
                    // todo: bindValue
                    property.setValue(supplier, v);
                }
            });
        }

        return BindingResult.of(supplier.get());
    }

    @Override
    public <T> BindingResult<T> bindValue(NamePath name, Bindable<T> bindable, DataSource source) {
        Object       result      = null;
        ObjectBinder rootBinder = context.getRootBinder();
        DataSource value = source.get(name);

        if (value.isNull()) {
            // todo: do something
        }

        if (bindable.getTypeDescriptor().isScalar()) {
            System.out.println("Simple Bindable");
            result = value.isNull() ? null : value.getRaw();
            System.out.println(result);
        } else {
            if (context.isDeepBinding()) {
                result = rootBinder.bind(name, bindable, source).getValue();
            }
        }

        return BindingResult.of((T)result);
    }

    @Override
    public  <T> boolean supports(Bindable<T> bindable) {
        // fallback binder always true
        return true;
    }

}
