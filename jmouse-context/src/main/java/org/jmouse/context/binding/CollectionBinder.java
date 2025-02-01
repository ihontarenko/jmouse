package org.jmouse.context.binding;

import org.jmouse.core.reflection.JavaType;
import org.jmouse.core.reflection.TypeDescriptor;

import java.util.Collection;
import java.util.function.Supplier;

import static org.jmouse.context.binding.Bindable.of;

abstract public class CollectionBinder extends AbstractBinder {

    public CollectionBinder(BindContext context) {
        super(context);
    }

    @Override
    public <T> BindingResult<T> bind(NamePath name, Bindable<T> bindable, DataSource source) {
        TypeDescriptor typeDescriptor = bindable.getTypeDescriptor();

        if (typeDescriptor.isCollection()) {
            int                index    = 0;
            Collection<Object> elements = (Collection<Object>) getCollection(bindable);

            while (true) {
                NamePath elementName = name.append("[" + index++ + "]");
                JavaType elementType = bindable.getType().getFirst();

                BindingResult<Object> result = bindValue(elementName, of(elementType), source);

                if (result.isEmpty()) {
                    break;
                }

                result.ifPresent(elements::add);
            }

            return BindingResult.of((T) elements);
        }

        return BindingResult.empty();
    }

    protected Collection<?> getCollection(Bindable<?> bindable) {
        TypeDescriptor typeDescriptor = bindable.getTypeDescriptor();
        Supplier<?>    supplier       = bindable.getValue();

        if (typeDescriptor.isCollection() && supplier != null) {
            if (supplier.get() instanceof Collection<?> collection) {
                return collection;
            }
        }

        return getCollectionSupplier().get();
    }

    abstract protected <T> Supplier<? extends Collection<T>> getCollectionSupplier();

}
