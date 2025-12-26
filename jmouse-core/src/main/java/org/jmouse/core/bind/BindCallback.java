package org.jmouse.core.bind;

/**
 * A callback interface for handling various stages of the binding process.
 * <p>
 * Implementations of this interface can intercept and modify the binding process at different
 * stages, including key resolution, value transformation, error handling, and tracking bound values.
 * </p>
 * <p>
 * By default, all methods provide no-op implementations, allowing selective overrides in custom implementations.
 * </p>
 *
 * @author JMouse - Team
 * @author Mr. Jerry Mouse
 * @author Ivan Hontarenko
 */
public interface BindCallback {

    /** A default no-op instance of {@link BindCallback}, which performs no modifications. */
    BindCallback NOOP = new BindCallback() {};

    /**
     * Resolves the property path before binding.
     * <p>
     * This method is invoked before the binding process begins and allows modification
     * or normalization of the property path.
     * </p>
     *
     * @param name    the original property path
     * @param context the binding context
     * @return the resolved property path
     */
    default PropertyPath onKeyCreated(PropertyPath name, BindContext context) {
        return name;
    }

    /**
     * Invoked before binding a value to a bindable target.
     * <p>
     * This method allows modification of the value before it is bound to the target.
     * </p>
     *
     * @param name     the property path
     * @param bindable the target bindable
     * @param context  the binding context
     * @param value    the value to be bound
     * @return the transformed or original value
     */
    default Object onBinding(PropertyPath name, Bindable<?> bindable, BindContext context, Object value) {
        return value;
    }

    /**
     * Invoked after a value has been successfully bound.
     * <p>
     * This method can be used to track bound values or perform additional post-processing.
     * </p>
     *
     * @param name     the property path
     * @param bindable the target bindable
     * @param context  the binding context
     * @param value    the result of the binding operation
     */
    default void onBound(PropertyPath name, Bindable<?> bindable, BindContext context, BindResult<Object> value) {
    }

    /**
     * Invoked when a binding operation results in no value being bound.
     * <p>
     * This method allows custom handling when no value is assigned to a bindable.
     * </p>
     *
     * @param name     the property path
     * @param bindable the target bindable
     * @param context  the binding context
     */
    default void onUnbound(PropertyPath name, Bindable<?> bindable, BindContext context) {
    }

    /**
     * Handles binding failures.
     * <p>
     * This method is invoked when an exception occurs during the binding process.
     * Implementations can provide custom error handling or recovery strategies.
     * </p>
     *
     * @param name      the property path
     * @param bindable  the target bindable
     * @param context   the binding context
     * @param exception the exception that occurred
     * @return a fallback value, or rethrows the exception if not handled
     * @throws Exception if the exception should be propagated
     */
    default Object onFailure(PropertyPath name, Bindable<?> bindable, BindContext context, Exception exception)
            throws Exception {
        throw exception;
    }

    /**
     * Determines whether this callback supports the given {@link ObjectBinder}.
     * <p>
     * This method allows filtering of callbacks based on the binder context.
     * The default implementation always returns {@code true}.
     * </p>
     *
     * @param binder the object binder to check
     * @return {@code true} if the callback supports the binder, {@code false} otherwise
     */
    default boolean supports(ObjectBinder binder) {
        return true;
    }

    /**
     * Sets the parent {@link BindCallback} for this callback.
     * <p>
     * Default implementation is a no-op, allowing callbacks that do not
     * require parent awareness to ignore this contract.
     * </p>
     *
     * @param callback the parent callback to associate with this instance
     */
    default void withParent(BindCallback callback) {}

}
