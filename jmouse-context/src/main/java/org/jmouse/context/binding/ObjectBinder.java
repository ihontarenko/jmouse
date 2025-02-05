package org.jmouse.context.binding;

/**
 * Defines the contract for binding objects from a {@link DataSource}.
 * Implementations of this interface are responsible for mapping data
 * from a source to a target bindable type.
 */
public interface ObjectBinder {

    /**
     * Binds the data associated with the given {@link NamePath} to a {@link Bindable} type.
     * This method attempts to construct an instance of the specified type using the available data.
     *
     * @param name     the hierarchical path representing the data key
     * @param bindable the target bindable type
     * @param source   the data source providing the values
     * @param <T>      the type of the bound object
     * @return a {@link BindResult} containing the bound object if successful
     */
    <T> BindResult<T> bind(NamePath name, Bindable<T> bindable, DataSource source);

    /**
     * Binds a single value from the {@link DataSource} to the given {@link Bindable} type.
     * Unlike {@link #bind(NamePath, Bindable, DataSource)}, this method is used to bind individual values
     * rather than constructing full objects.
     *
     * @param name     the hierarchical path representing the data key
     * @param bindable the target bindable type
     * @param source   the data source providing the value
     * @param <T>      the type of the bound value
     * @return a {@link BindResult} containing the bound value if present
     */
    <T> BindResult<T> bindValue(NamePath name, Bindable<T> bindable, DataSource source);

    /**
     * Determines whether this binder supports binding for the given {@link Bindable} type.
     * By default, binders do not support any types unless explicitly overridden.
     *
     * @param bindable the bindable type to check
     * @param <T>      the type parameter
     * @return {@code true} if this binder supports the given type, {@code false} otherwise
     */
    default <T> boolean supports(Bindable<T> bindable) {
        return false;
    }
}
