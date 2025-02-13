package org.jmouse.core.bind;

/**
 * A callback interface for handling various stages of the binding process.
 * Implementations can customize how keys are resolved, values are transformed,
 * and errors are handled during data binding.
 */
public interface BindCallback {

    /** A default no-op instance of {@link BindCallback}. */
    BindCallback NOOP = new BindCallback() {};

    /**
     * Resolves the key name before binding.
     */
    default PropertyPath onKeyCreated(PropertyPath name, BindContext context) {
        return name;
    }

    /**
     * Invoked before binding a value to a bindable.
     */
    default Object onBinding(PropertyPath name, Bindable<?> bindable, BindContext context, Object value) {
        return value;
    }

    /**
     * Invoked after a value has been successfully bound.
     */
    default void onBound(PropertyPath name, Bindable<?> bindable, BindContext context, BindResult<Object> value) {
    }

    /**
     * Handles binding failures.
     */
    default Object onFailure(PropertyPath name, Bindable<?> bindable, BindContext context, Exception exception)
            throws Exception {
        throw exception;
    }
}
