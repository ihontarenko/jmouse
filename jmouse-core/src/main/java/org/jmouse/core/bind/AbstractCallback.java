package org.jmouse.core.bind;

/**
 * Abstract base class for binding callbacks.
 * <p>
 * This class provides a base implementation for {@link BindCallback} with support for delegation
 * to a parent callback. It allows chaining of binding operations where multiple callbacks
 * can be executed in sequence.
 * </p>
 *
 * @author JMouse - Team
 * @author Mr. Jerry Mouse
 * @author Ivan Hontarenko
 */
public abstract class AbstractCallback implements BindCallback {

    /** The parent callback to delegate to, if applicable. */
    protected BindCallback parent;

    /**
     * Constructs an {@code AbstractCallback} with a specified parent callback.
     *
     * @param parent the parent {@link BindCallback} to delegate to
     */
    protected AbstractCallback(BindCallback parent) {
        this.parent = parent;
    }

    /**
     * Constructs an {@code AbstractCallback} with a default parent callback (NOOP).
     */
    protected AbstractCallback() {
        this(NOOP);
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
    @Override
    public void withParent(BindCallback callback) {
        parent = callback;
    }

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
    @Override
    public PropertyPath onKeyCreated(PropertyPath name, BindContext context) {
        return parent == null
                ? BindCallback.super.onKeyCreated(name, context)
                : parent.onKeyCreated(name, context);
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
    @Override
    public Object onBinding(PropertyPath name, Bindable<?> bindable, BindContext context, Object value) {
        return parent == null
                ? BindCallback.super.onBinding(name, bindable, context, value)
                : parent.onBinding(name, bindable, context, value);
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
    @Override
    public void onBound(PropertyPath name, Bindable<?> bindable, BindContext context, BindResult<Object> value) {
        if (parent != null) {
            parent.onBound(name, bindable, context, value);
        }
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
    @Override
    public void onUnbound(PropertyPath name, Bindable<?> bindable, BindContext context) {
        if (parent != null) {
            parent.onUnbound(name, bindable, context);
        }
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
    @Override
    public Object onFailure(PropertyPath name, Bindable<?> bindable, BindContext context, Exception exception)
            throws Exception {
        return parent == null
                ? BindCallback.super.onFailure(name, bindable, context, exception)
                : parent.onFailure(name, bindable, context, exception);
    }
}
