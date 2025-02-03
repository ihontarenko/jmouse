package org.jmouse.context.binding;

import org.jmouse.core.reflection.JavaType;
import org.jmouse.core.reflection.TypeDescriptor;

import java.util.Collection;
import java.util.function.Supplier;

import static org.jmouse.context.binding.Bindable.of;

/**
 * The {@code CollectionBinder} class is an abstract binder for handling collection types.
 * It binds each element of a collection in a recursive manner, ensuring that each element is processed individually
 * based on the provided path.
 * <p>
 * This binder works with collections and binds the elements of the collection to their respective names based on
 * their index in the collection. If no elements are found or cannot be bound, the result is considered empty.
 * </p>
 */
abstract public class CollectionBinder extends AbstractBinder {

    private static final String INDEX_ZERO = "[0]";

    /**
     * Constructs a new {@code CollectionBinder} with the given context.
     *
     * @param context the binding context to be used for binding
     */
    public CollectionBinder(BindContext context) {
        super(context);
    }

    /**
     * Binds a collection to the provided path using the specified {@link Bindable} object and data source.
     * The collection elements are bound recursively, and their individual values are added to the collection.
     * <p>
     * The binding process continues until all elements in the collection are processed or until no further
     * valid results are found.
     * </p>
     *
     * @param <T> the type of the object to bind
     * @param root the path name for binding
     * @param bindable the bindable object representing the collection
     * @param source the data source
     * @return the binding result containing the collection, or an empty result if no binding is possible
     */
    @Override
    public <T> BindingResult<T> bind(NamePath root, Bindable<T> bindable, DataSource source) {
        TypeDescriptor typeDescriptor = bindable.getTypeDescriptor();

        // Check if the bindable is a collection
        if (typeDescriptor.isCollection()) {
            @SuppressWarnings({"unchecked"}) Collection<Object> elements = (Collection<Object>) getCollection(bindable);
            int index   = 0;
            int maxSize = 16; // todo: move this value to context

            JavaType elementType = bindable.getType().getFirst();
            NamePath zeroName    = root.append(INDEX_ZERO);

            if (source.get(zeroName).isNull() && source.get(root).isSimple()) {
                bindCollectionElement(root, of(elementType), source, elements);
            } else {
                while (maxSize > index) {
                    // Append the index to the path for element binding
                    NamePath elementName = root.append("[" + index++ + "]");

                    BindingResult<?> result = bindCollectionElement(elementName, of(elementType), source, elements);

                    if (result.isEmpty()) {
                        break;
                    }
                }
            }

            return BindingResult.of((T) elements);
        }

        // Return an empty result if not a collection
        return BindingResult.empty();
    }

    private BindingResult<?> bindCollectionElement(NamePath name, Bindable<?> bindable, DataSource source, Collection<Object> elements) {
        // Bind the individual element and add it to the collection
        BindingResult<?> result = bindValue(name, bindable, source);

        // Add the successfully bound element to the collection
        result.ifPresent(elements::add);

        return result;
    }

    /**
     * Retrieves the collection associated with the provided {@link Bindable} object.
     * If the bindable's value is a collection, it returns it; otherwise, it returns a default collection from
     * a supplier.
     *
     * @param bindable the bindable object representing the collection
     * @return the collection associated with the bindable object, or a default collection if none is found
     */
    protected Collection<?> getCollection(Bindable<?> bindable) {
        TypeDescriptor typeDescriptor = bindable.getTypeDescriptor();
        Supplier<?>    supplier       = bindable.getValue();

        // Check if the bindable represents a collection and return the value if available
        if (typeDescriptor.isCollection() && supplier != null) {
            if (supplier.get() instanceof Collection<?> collection) {
                return collection;
            }
        }

        // Return the default collection from the supplier
        return getCollectionSupplier(bindable).get();
    }

    /**
     * Retrieves a supplier for the default collection. This is used when the bindable does not provide a collection.
     * The subclass should provide an implementation for this method to specify how to retrieve the default collection.
     *
     * @param <T> the type of elements in the collection
     * @return a supplier that provides the default collection
     */
    abstract protected <T> Supplier<? extends Collection<T>> getCollectionSupplier(Bindable<T> bindable);
}

