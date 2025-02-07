package org.jmouse.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexPlaceholderReplacer implements PlaceholderReplacer {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{(.+?)}");

    /**
     * Replaces placeholders in the given string using a {@link PlaceholderResolver}.
     *
     * @param value    the input string containing placeholders
     * @param resolver the resolver used to retrieve placeholder values
     * @return the string with resolved placeholders
     */
    @Override
    public String replace(String value, PlaceholderResolver resolver) {
        Matcher       matcher = PLACEHOLDER_PATTERN.matcher(value);
        StringBuilder builder = new StringBuilder();

        while (matcher.find()) {
            String placeholder = matcher.group(1);
            String result      = resolver.resolvePlaceholder(placeholder);
            matcher.appendReplacement(builder, Matcher.quoteReplacement(result));
        }

        matcher.appendTail(builder);

        return builder.toString();
    }
}
