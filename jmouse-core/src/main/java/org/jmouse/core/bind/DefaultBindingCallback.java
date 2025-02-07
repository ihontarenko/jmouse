package org.jmouse.core.bind;

import org.jmouse.util.PlaceholderReplacer;
import org.jmouse.util.PlaceholderResolver;
import org.jmouse.util.StandardPlaceholderReplacer;

public class DefaultBindingCallback implements BindCallback {

    private final PlaceholderReplacer replacer;

    public DefaultBindingCallback(PlaceholderReplacer replacer) {
        this.replacer = replacer;
    }

    public DefaultBindingCallback() {
        this.replacer = new StandardPlaceholderReplacer();
    }

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

        return handled;
    }

}
