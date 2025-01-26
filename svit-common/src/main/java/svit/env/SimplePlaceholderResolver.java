package svit.env;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SimplePlaceholderResolver implements PlaceholderResolver {

    private static final Pattern          PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{(.+?)}");
    private final        PropertyResolver resolver;

    public SimplePlaceholderResolver(PropertyResolver resolver) {
        this.resolver = resolver;
    }

    /**
     * Resolves placeholders in the given text.
     */
    @Override
    public String resolvePlaceholders(String text) {
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(text);
        StringBuilder builder = new StringBuilder();

        while (matcher.find()) {
            String placeholder = matcher.group(1);
            String value = resolver.getProperty(placeholder);
            matcher.appendReplacement(builder, Matcher.quoteReplacement(value));
        }

        matcher.appendTail(builder);

        return builder.toString();
    }

    /**
     * Resolves placeholders, throwing an exception if any remain unresolved.
     */
    @Override
    public String resolveRequiredPlaceholders(String text) {
        String resolved = resolvePlaceholders(text);

        if (resolved.contains("${")) {
            throw new UnresolvedPropertyPlaceholderException("Unresolved placeholder '%s' is left".formatted(resolved));
        }

        return resolved;
    }

}
