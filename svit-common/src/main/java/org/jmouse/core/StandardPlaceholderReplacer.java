package org.jmouse.core;

import org.jmouse.util.Visitor;

import java.util.Properties;

public class StandardPlaceholderReplacer implements PlaceholderReplacer {

    private final String          prefix;
    private final String          suffix;
    private final Visitor<String> visitor;

    public StandardPlaceholderReplacer(String prefix, String suffix) {
        this.prefix = prefix;
        this.suffix = suffix;
        this.visitor = new Visitor.Default<>();
    }

    public StandardPlaceholderReplacer() {
        this(PLACEHOLDER_PREFIX, PLACEHOLDER_SUFFIX);
    }

    public String replace(String value, Properties properties) {
        return replace(value, properties::getProperty);
    }

    @Override
    public String replace(String value, PlaceholderResolver resolver) {
        return parse(value, resolver);
    }

    private String parse(String value, PlaceholderResolver resolver) {
        int prefixIndex = value.indexOf(prefix);

        if (prefixIndex == -1) {
            return value;
        }

        while (prefixIndex != -1) {
            int suffixIndex = value.indexOf(suffix, prefixIndex);

            if (suffixIndex != -1) {
                String placeholder = value.substring(prefixIndex + prefix.length(), suffixIndex);
                System.out.println(placeholder);
            } else {
                prefixIndex = -1;
            }
        }

        StringBuilder result = new StringBuilder(value);

        return value;
    }


}
