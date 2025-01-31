package org.jmouse.context.binding;

import org.jmouse.core.reflection.JavaType;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

public class ArrayBinder extends CollectionBinder {

    public ArrayBinder(BindContext context) {
        super(context);
    }

    @Override
    public <T> BindingResult<T> bind(NamePath name, Bindable<T> bindable, DataSource source) {
        JavaType         elementType = bindable.getType().getComponentType();
        JavaType         type        = JavaType.forParametrizedClass(List.class, elementType.getRawType());
        BindingResult<T> result      = super.bind(name, Bindable.of(type), source);

        // resulted array object
        T elements = null;

        if (result.isPresent()) {
            if (result.getValue() instanceof List<?> arrayList) {
                Object array = Array.newInstance(elementType.getRawType(), arrayList.size());

                for (int i = 0; i < arrayList.size(); i++) {
                    Array.set(array, i, arrayList.get(i));
                }

                elements = (T) array;
            }
        }

        return BindingResult.of(elements);
    }

    @Override
    public <T> boolean supports(Bindable<T> bindable) {
        return bindable.getTypeDescriptor().isArray();
    }

    @Override
    protected <T> Supplier<? extends Collection<T>> getCollection() {
        return ArrayList::new;
    }
}
