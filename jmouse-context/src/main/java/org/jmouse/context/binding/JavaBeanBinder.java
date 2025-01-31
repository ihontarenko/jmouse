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
            JavaType   propertyType = property.getType();
            String     propertyName = property.getName();
            DataSource value        = source.get(name.append(propertyName));

            if (property.isWritable()) {
                property.setValue(supplier, value.getRaw());
            }
        }

        return BindingResult.of(supplier.get());
    }

    @Override
    public <T> BindingResult<T> bindValue(NamePath name, Bindable<T> bindable, DataSource source) {
        if (source.isSimple()) {

        }
        return BindingResult.of(null);
    }

    @Override
    public  <T> boolean supports(Bindable<T> bindable) {
        // fallback binder always true
        return true;
    }

}
