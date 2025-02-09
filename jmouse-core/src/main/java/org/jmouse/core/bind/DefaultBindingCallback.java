package org.jmouse.core.bind;

import org.jmouse.core.reflection.TypeDescriptor;
import org.jmouse.util.PlaceholderReplacer;
import org.jmouse.util.PlaceholderResolver;
import org.jmouse.util.StandardPlaceholderReplacer;

/**
 * Default implementation of {@link AbstractCallback} that provides placeholder resolution
 * and additional handling for bound values.
 * <p>
 * This callback:
 * <ul>
 *   <li>Supports placeholder replacement in {@link String} values.</li>
 *   <li>Delegates additional processing to the parent callback.</li>
 * </ul>
 */
public class DefaultBindingCallback extends AbstractCallback {

    private final PlaceholderReplacer replacer;

    /**
     * Creates a new instance with a custom {@link PlaceholderReplacer}.
     *
     * @param replacer the placeholder replacer to use
     */
    public DefaultBindingCallback(PlaceholderReplacer replacer) {
        this.replacer = replacer;
    }

    /**
     * Creates a new instance using the {@link StandardPlaceholderReplacer}.
     */
    public DefaultBindingCallback() {
        this.replacer = new StandardPlaceholderReplacer();
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
    public void onBound(NamePath name, Bindable<?> bindable, BindContext context, BindResult<Object> value) {
        parent.onBound(name, bindable, context, value);
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
    public Object onBinding(NamePath name, Bindable<?> bindable, BindContext context, Object value) {
        Object handled = value;

        if (handled instanceof String stringValue && bindable.getTypeDescriptor().isString()) {
            String prefix = replacer.prefix();
            if (stringValue.contains(prefix)) {
                PlaceholderResolver resolver = (placeholder) -> {
                    DataSource dataSource = context.getDataSource();
                    NamePath   path       = NamePath.of(placeholder);
                    DataSource other      = dataSource.get(path);
                    return other.isNull() ? null : other.asString();
                };
                handled = replacer.replace(stringValue, resolver);
            }
        }

        return parent.onBinding(name, bindable, context, handled);
    }

}
