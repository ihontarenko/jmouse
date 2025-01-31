package org.jmouse.context.binding;

import org.jmouse.core.reflection.JavaType;
import org.jmouse.core.reflection.TypeDescriptor;

import java.util.Collection;
import java.util.function.Supplier;

abstract public class CollectionBinder extends AbstractBinder {

    public CollectionBinder(BindContext context) {
        super(context);
    }

    @Override
    public <T> BindingResult<T> bind(NamePath name, Bindable<T> bindable, DataSource source) {
        DataSource     collection     = source.get(name);
        TypeDescriptor typeDescriptor = bindable.getTypeDescriptor();

        if (typeDescriptor.isCollection()) {
            int index = 0;
            Collection<Object> elements = getCollection().get();

            for (Object element : collection.asCollection()) {
                NamePath elementName = name.append("[" + index++ + "]");
                JavaType elementType = bindable.getType().getFirst();

                BindingResult<Object> result = bindValue(elementName, Bindable.of(elementType.getRawType()), source);

                result.ifPresent(elements::add);
            }

            return BindingResult.of((T) elements);
        }

        return BindingResult.empty();
    }

    abstract protected <T> Supplier<? extends Collection<T>> getCollection();

}
