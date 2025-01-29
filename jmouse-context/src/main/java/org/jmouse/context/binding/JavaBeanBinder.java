package org.jmouse.context.binding;

import org.jmouse.core.reflection.JavaType;

import java.util.function.Supplier;

public class JavaBeanBinder extends AbstractBinder {

    @Override
    public <T> BindingResult<T> bind(PropertyName name, Bindable<T> bindable, DataSource source) {
        Class<T> type = (Class<T>) bindable.getType().getRawType();
        Bean<T>  bean = Bean.of(type);
        Supplier<T> supplier = bean.getSupplier(bindable);

        for (Bean.Property property : bean.getProperties()) {
            property.setValue(supplier, source.get(property.getName()).as(property.getType().getRawType()));
        }

        return BindingResult.of(null);
    }

    @Override
    public boolean supports(JavaType type) {
        // fallback binder always true
        return true;
    }

}
