package org.jmouse.core.bind;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Supplier;

import static org.jmouse.core.reflection.ClassMatchers.isSupertype;

/**
 * {@code ListBinder} is a specific implementation of {@link CollectionBinder} that handles binding for {@link List} types.
 * This binder supports binding of {@link List} collections, allowing individual elements to be bound to their
 * respective paths and added to the list.
 * <p>
 * The class overrides methods from {@code CollectionBinder} to provide the necessary support for lists, including
 * checking whether the bindable type is a list and providing a supplier to create new lists.
 * </p>
 */
public class ListBinder extends CollectionBinder {

    /**
     * Constructs a new {@code ListBinder} with the given binding context.
     *
     * @param context the binding context to be used for binding
     */
    public ListBinder(BindContext context) {
        super(context);
    }

    /**
     * Checks if the given {@link Bindable} object is of type {@link List}.
     * This method ensures that the binder is used only when the bindable object is of a compatible type.
     *
     * @param <T> the type of the bindable object
     * @param bindable the bindable object to check
     * @return {@code true} if the bindable type is a {@link List}, {@code false} otherwise
     */
    @Override
    public <T> boolean supports(Bindable<T> bindable) {
        return isSupertype(List.class).matches(bindable.getType().getRawType());
    }

    /**
     * Provides a supplier that creates a new {@link LinkedList}.
     * This method is used to create the collection of elements before converting it into an array.
     *
     * @param <T> the type of elements in the collection
     * @param bindable the bindable object for which the collection supplier is provided
     * @return a supplier that creates a new {@link LinkedList}
     */
    @Override
    protected <T> Supplier<? extends Collection<T>> getCollectionSupplier(Bindable<T> bindable) {
        return LinkedList::new;
    }
}

