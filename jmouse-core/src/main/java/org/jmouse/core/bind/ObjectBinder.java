package org.jmouse.core.bind;

/**
 * Defines the contract for binding objects from a {@link ObjectAccessor}.
 * Implementations of this interface are responsible for mapping data
 * from a source to a target bindable type, optionally applying a {@link BindCallback}.
 */
public interface ObjectBinder {

    /**
     * Binds a value from the given {@link ObjectAccessor} to a target {@link TypedValue} type.
     *
     * @param name     the property path in the data accessor
     * @param bindable the target bindable type
     * @param accessor   the data accessor providing values
     * @param callback the binding callback for customization
     * @param <T>      the expected type of the bound value
     * @return a {@link BindResult} containing the bound value, or an empty result if not found
     */
    <T> BindResult<T> bind(PropertyPath name, TypedValue<T> bindable, ObjectAccessor accessor, BindCallback callback);

    /**
     * Binds a single value from the given {@link ObjectAccessor} to a target {@link TypedValue} type.
     * This method is used for simple scalar values rather than complex object binding.
     *
     * @param name     the property path in the data accessor
     * @param bindable the target bindable type
     * @param accessor   the data accessor providing values
     * @param callback the binding callback for customization
     * @param <T>      the expected type of the bound value
     * @return a {@link BindResult} containing the bound value, or an empty result if not found
     */
    <T> BindResult<T> bindValue(PropertyPath name, TypedValue<T> bindable, ObjectAccessor accessor, BindCallback callback);

    /**
     * Determines whether this binder supports binding the given {@link TypedValue} type.
     *
     * @param bindable the bindable type to check
     * @param <T>      the type of the bindable
     * @return {@code true} if this binder supports binding the given type, otherwise {@code false}
     */
    default <T> boolean supports(TypedValue<T> bindable) {
        return false;
    }
}
