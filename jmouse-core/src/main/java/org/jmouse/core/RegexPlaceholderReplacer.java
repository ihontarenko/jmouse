package org.jmouse.core;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexPlaceholderReplacer implements PlaceholderReplacer {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{(.+?)}");

    /**
     * Replaces placeholders in the given string using a {@link PlaceholderResolver}.
     */
    @Override
    public String replace(String value, PlaceholderResolver resolver) {
        Matcher       matcher = PLACEHOLDER_PATTERN.matcher(value);
        StringBuilder builder = new StringBuilder();

        while (matcher.find()) {
            String placeholder = matcher.group(1);
            String result      = resolver.resolvePlaceholder(placeholder, null);
            matcher.appendReplacement(builder, Matcher.quoteReplacement(result));
        }

        matcher.appendTail(builder);

        return builder.toString();
    }

    @Override
    public String suffix() {
        return PLACEHOLDER_SUFFIX;
    }

    @Override
    public String prefix() {
        return PLACEHOLDER_PREFIX;
    }
}
