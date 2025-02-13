package org.jmouse.core.bind;

import org.jmouse.core.reflection.JavaType;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

/**
 * {@code ArrayBinder} is a specific implementation of {@link CollectionBinder} that handles binding for array types.
 * This binder supports binding of arrays by first converting them into a list, binding the individual elements,
 * and then converting the list back into an array.
 * <p>
 * The class overrides methods from {@code CollectionBinder} to provide the necessary support for arrays, including
 * checking whether the bindable type is an array and providing a supplier to create a collection of elements before
 * converting it back into an array.
 * </p>
 */
public class ArrayBinder extends CollectionBinder {

    /**
     * Constructs a new {@code ArrayBinder} with the given binding context.
     *
     * @param context the binding context to be used for binding
     */
    public ArrayBinder(BindContext context) {
        super(context);
    }

    /**
     * Binds the elements of an array. First, the method binds the array as a list, then converts the list back into
     * an array of the correct type. It creates an array instance using reflection and sets the elements based on the
     * list contents.
     *
     * @param <T> the type of the bindable object
     * @param root the name path to be used for the binding
     * @param bindable the bindable object that holds the type and value to be bound
     * @param source the data source from which to fetch values
     * @param callback the binding callback for customization
     * @return a {@link BindResult} containing the bound array, or an empty result if no valid binding was found
     */
    @Override
    public <T> BindResult<T> bind(PropertyPath root, Bindable<T> bindable, PropertyValueAccessor source, BindCallback callback) {
        JavaType      elementType = bindable.getType().getComponentType();
        JavaType      type        = JavaType.forParametrizedClass(List.class, elementType.getRawType());
        BindResult<T> result      = super.bind(root, Bindable.of(type), source, callback);

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

        return BindResult.of(elements);
    }

    /**
     * Checks if the given {@link Bindable} object is an array. This method ensures that the binder is used only
     * when the bindable object is of a compatible array type.
     *
     * @param <T> the type of the bindable object
     * @param bindable the bindable object to check
     * @return {@code true} if the bindable type is an array, {@code false} otherwise
     */
    @Override
    public <T> boolean supports(Bindable<T> bindable) {
        return bindable.getTypeInformation().isArray();
    }

    /**
     * Provides a supplier that creates a new {@link ArrayList}.
     * This method is used to create the collection of elements before converting it into an array.
     *
     * @param <T> the type of elements in the collection
     * @param bindable the bindable object for which the collection supplier is provided
     * @return a supplier that creates a new {@link ArrayList}
     */
    @Override
    protected <T> Supplier<? extends Collection<T>> getCollectionSupplier(Bindable<T> bindable) {
        return ArrayList::new;
    }
}

