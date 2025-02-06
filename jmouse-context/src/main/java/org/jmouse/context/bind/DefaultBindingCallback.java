package org.jmouse.context.bind;

import org.jmouse.core.PlaceholderReplacer;
import org.jmouse.core.PlaceholderResolver;
import org.jmouse.core.StandardPlaceholderReplacer;

public class DefaultBindingCallback implements BindCallback {

    private final PlaceholderReplacer replacer;

    public DefaultBindingCallback(PlaceholderReplacer replacer) {
        this.replacer = replacer;
    }

    @Override
    public Object onBinding(NamePath name, Bindable<?> bindable, BindContext context, Object value) {
        Object handled = value;

        if (handled instanceof String stringValue && bindable.getTypeDescriptor().isString()) {
            String prefix = PlaceholderReplacer.PLACEHOLDER_PREFIX;
            if (replacer instanceof StandardPlaceholderReplacer standardReplacer) {
                prefix = standardReplacer.getPrefix();
            }

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

        return handled;
    }

}
