package org.jmouse.core.binding;

import org.jmouse.core.Priority;
import org.jmouse.core.access.TypedValue;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Supplier;

/**
 * {@code SetBinder} is a specific implementation of {@link CollectionBinder} that handles binding for {@link Set} types.
 * This binder supports binding of {@link Set} collections by providing a supplier to create a new {@link LinkedHashSet}
 * and ensuring that the bindable type is compatible with a {@link Set}.
 * <p>
 * The class overrides methods from {@code CollectionBinder} to provide the necessary support for sets, including
 * checking whether the bindable type is a {@link Set} and providing a supplier to create a collection.
 * </p>
 */
@Priority(SetBinder.PRIORITY)
public class SetBinder extends CollectionBinder {

    public static final int PRIORITY = ListBinder.PRIORITY + 10;

    /**
     * Constructs a new {@code SetBinder} with the given binding context.
     *
     * @param context the binding context to be used for binding
     */
    public SetBinder(BindContext context) {
        super(context);
    }

    /**
     * Checks if the given {@link TypedValue} object is a {@link Set}. This method ensures that the binder is used
     * only when the bindable object is of a compatible {@link Set} type.
     *
     * @param <T> the type of the bindable object
     * @param bindable the bindable object to check
     * @return {@code true} if the bindable type is a {@link Set}, {@code false} otherwise
     */
    @Override
    public <T> boolean supports(TypedValue<T> bindable) {
        return bindable.getType().isSet();
    }

    /**
     * Provides a supplier that creates a new {@link LinkedHashSet}.
     * This method is used to create the collection of elements for binding.
     *
     * @param <T> the type of elements in the collection
     * @param bindable the bindable object for which the collection supplier is provided
     * @return a supplier that creates a new {@link LinkedHashSet}
     */
    @Override
    protected <T> Supplier<? extends Collection<T>> getCollectionSupplier(TypedValue<T> bindable) {
        return LinkedHashSet::new;
    }
}

