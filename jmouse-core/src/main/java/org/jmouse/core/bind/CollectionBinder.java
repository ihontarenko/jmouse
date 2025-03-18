package org.jmouse.core.bind;

import org.jmouse.core.reflection.JavaType;
import org.jmouse.core.reflection.TypeInformation;

import java.util.Collection;
import java.util.function.Supplier;

import static org.jmouse.core.bind.Bindable.of;

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
     * @param accessor the data source
     * @param callback the binding callback for customization
     * @return the binding result containing the collection, or an empty result if no binding is possible
     */
    @Override
    public <T> BindResult<T> bind(PropertyPath root, Bindable<T> bindable, ObjectAccessor accessor, BindCallback callback) {
        TypeInformation typeDescriptor = bindable.getTypeInformation();

        // Check if the bindable is a collection
        if (typeDescriptor.isCollection()) {
            @SuppressWarnings({"unchecked"}) Collection<Object> elements = (Collection<Object>) getCollection(bindable);
            int index   = 0;
            int maxSize = 16; // todo: move this value to context

            JavaType     elementType = bindable.getType().getFirst();
            PropertyPath zeroName    = root.append(INDEX_ZERO);

            if (accessor.navigate(zeroName).isNull() && accessor.navigate(root).isSimple()) {
                bindCollectionElement(root, of(elementType), accessor, elements, callback);
            } else {
                while (maxSize > index) {
                    // Append the index to the path for element binding
                    PropertyPath elementName = root.append("[" + index++ + "]");

                    BindResult<?> result = bindCollectionElement(
                            elementName, of(elementType), accessor, elements, callback);

                    if (result.isEmpty()) {
                        break;
                    }
                }
            }

            return BindResult.of((T) elements);
        }

        // Return an empty result if not a collection
        return BindResult.empty();
    }

    private BindResult<?> bindCollectionElement(
            PropertyPath name, Bindable<?> bindable, ObjectAccessor source, Collection<Object> elements, BindCallback callback) {
        // Bind the individual element and add it to the collection
        BindResult<?> result = bindValue(name, bindable, source, callback);

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
        TypeInformation typeDescriptor = bindable.getTypeInformation();
        Supplier<?>     supplier       = bindable.getValue();

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

