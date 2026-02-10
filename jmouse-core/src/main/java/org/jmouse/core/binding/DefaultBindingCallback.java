package org.jmouse.core.binding;

import org.jmouse.core.PlaceholderReplacer;
import org.jmouse.core.PlaceholderResolver;
import org.jmouse.core.StandardPlaceholderReplacer;
import org.jmouse.core.access.ObjectAccessor;
import org.jmouse.core.access.PropertyPath;
import org.jmouse.core.access.TypedValue;

/**
 * DirectAccess implementation of a binding callback used in property binding operations.
 * <p>
 * This class extends {@link AbstractCallback} and provides a default implementation
 * for handling placeholder replacement within binding operations.
 * </p>
 *
 * @author JMouse - Team
 * @author Mr. Jerry Mouse
 * @author Ivan Hontarenko
 */
public class DefaultBindingCallback extends AbstractCallback {

    private final PlaceholderReplacer replacer;

    /**
     * Constructs a {@code DefaultBindingCallback} with a specified parent callback and placeholder replacer.
     *
     * @param parent   the parent {@link BindCallback} to delegate to
     * @param replacer the {@link PlaceholderReplacer} used for handling placeholders
     */
    public DefaultBindingCallback(BindCallback parent, PlaceholderReplacer replacer) {
        this.replacer = replacer;
        this.parent = parent;
    }

    /**
     * Constructs a {@code DefaultBindingCallback} with a specified parent callback
     * and a default {@link StandardPlaceholderReplacer}.
     *
     * @param parent the parent {@link BindCallback} to delegate to
     */
    public DefaultBindingCallback(BindCallback parent) {
        this(parent, new StandardPlaceholderReplacer());
    }

    /**
     * Constructs a {@code DefaultBindingCallback} with a default parent callback (NOOP)
     * and a default {@link StandardPlaceholderReplacer}.
     */
    public DefaultBindingCallback() {
        this(NOOP, new StandardPlaceholderReplacer());
    }

    /**
     * Called when a value has been successfully bound.
     * <p>
     * If the bound type is a {@code record}, additional processing could be added.
     * </p>
     *
     * @param name     the property path
     * @param bindable the bindable type being processed
     * @param context  the binding context
     * @param value    the bound value result
     */
    @Override
    public void onBound(PropertyPath name, TypedValue<?> bindable, BindContext context, BindResult<Object> value) {
        parent.onBound(name, bindable, context, value);
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
    public void onUnbound(PropertyPath name, TypedValue<?> bindable, BindContext context) {
        super.onUnbound(name, bindable, context);
    }

    /**
     * Called before binding a value, allowing transformation before assignment.
     * <p>
     * If the value is a {@link String} containing placeholders, it attempts to resolve them
     * using the {@link PlaceholderReplacer}.
     * </p>
     *
     * @param name     the property path
     * @param bindable the bindable type
     * @param context  the binding context
     * @param value    the original value to bind
     * @return the transformed or resolved value
     */
    @Override
    public Object onBinding(PropertyPath name, TypedValue<?> bindable, BindContext context, Object value) {
        Object handled = value;

        if (handled instanceof String stringValue && bindable.getTypeInformation().isString()) {
            String prefix = replacer.prefix();
            if (stringValue.contains(prefix)) {
                PlaceholderResolver resolver = (placeholder, defaultValue) -> {
                    ObjectAccessor accessor = context.getObjectAccessor();
                    PropertyPath   path     = PropertyPath.forPath(placeholder);
                    ObjectAccessor other    = accessor.navigate(path);
                    return other.isNull() ? defaultValue : other.asText();
                };
                handled = replacer.replace(stringValue, resolver);
            }
        }

        return parent.onBinding(name, bindable, context, handled);
    }

}
