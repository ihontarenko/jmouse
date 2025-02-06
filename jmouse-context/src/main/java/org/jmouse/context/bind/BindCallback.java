package org.jmouse.context.bind;

/**
 * A callback interface for handling various stages of the binding process.
 * Implementations can customize how keys are resolved, values are transformed,
 * and errors are handled during data binding.
 */
public interface BindCallback {

    /** A default no-op instance of {@link BindCallback}. */
    BindCallback NULL = new BindCallback() {};

    /**
     * Resolves the key name before binding.
     *
     * @param name the original property path
     * @return the resolved property path (can be modified)
     */
    default NamePath onResolveKey(NamePath name) {
        return name;
    }

    /**
     * Invoked before binding a value to a bindable.
     *
     * @param name    the property path
     * @param bindable  the bindable type
     * @param context the binding context
     * @param value   the value to bind
     * @return the transformed value to bind (can be modified)
     */
    default Object onBinding(NamePath name, Bindable<?> bindable, BindContext context, Object value) {
        return value;
    }

    /**
     * Invoked after a value has been successfully bound.
     *
     * @param name    the property path
     * @param bindable  the bindable type
     * @param context the binding context
     * @param value   the bound value
     */
    default void onBound(NamePath name, Bindable<?> bindable, BindContext context, Object value) {
    }

    /**
     * Handles binding failures.
     *
     * @param name    the property path
     * @param bindable  the bindable type
     * @param context the binding context
     * @param error   the exception that occurred during binding
     * @return a fallback value (optional) or rethrows the exception
     * @throws Exception if the error should be propagated
     */
    default Object onFailure(NamePath name, Bindable<?> bindable, BindContext context, Exception error) throws Exception {
        throw error;
    }
}
