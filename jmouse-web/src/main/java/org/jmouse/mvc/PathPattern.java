package org.jmouse.mvc;

import org.jmouse.util.PlaceholderReplacer;
import org.jmouse.util.StandardPlaceholderReplacer;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 🧭 Parses path patterns like <code>/user/{id:\d+}/status/{status}</code> into regular expressions
 * and extracts matched parameters into {@link RoutePath}.
 * <p>
 * 💡 Example:
 * <pre>{@code
 * PathPattern pattern = new PathPattern("/user/{id:\\d+}/status/{status}");
 * RoutePath route = pattern.parse("/user/42/status/ACTIVE");
 * route.get("id"); // -> "42"
 * route.get("status"); // -> "ACTIVE"
 * }</pre>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class PathPattern {

    private static final PlaceholderReplacer REPLACER  = new StandardPlaceholderReplacer("{", "}", ":");
    private static final String              MATCH_ALL = "[^/]+";

    private final String          pattern;
    private final Pattern         expression;
    private final List<Parameter> parameters = new ArrayList<>();

    /**
     * Constructs a {@code PathPattern} by compiling the given template.
     *
     * @param pattern route pattern, e.g., {@code /user/{id:\d+}/status/{status}}
     */
    public PathPattern(String pattern) {
        this.pattern = pattern;
        this.expression = compile(pattern);
    }

    /**
     * Parses the given input URI and extracts path parameters.
     *
     * @param input URI string, e.g., {@code /user/42/status/ACTIVE}
     * @return parsed {@link RoutePath} with extracted variables
     */
    public RoutePath parse(String input) {
        Matcher             matcher   = expression.matcher(input);
        Map<String, Object> variables = new LinkedHashMap<>(8);

        if (matcher.matches()) {
            for (Parameter parameter : parameters) {
                variables.put(parameter.name(), matcher.group(parameter.name()));
            }
        }

        return new RoutePath(input, variables);
    }

    /**
     * Checks if the input URI matches this pattern.
     *
     * @param input URI string to match
     * @return {@code true} if pattern matches, {@code false} otherwise
     */
    public boolean matches(String input) {
        return expression.matcher(input).matches();
    }

    /**
     * Compiles the pattern into a named group regex.
     */
    private Pattern compile(String pattern) {
        return Pattern.compile("^%s$".formatted(REPLACER.replace(pattern, this::replace)));
    }

    /**
     * Replaces a placeholder with a named capturing regex group.
     */
    private String replace(String name, String defaultValue) {
        parameters.add(new Parameter(name, false));
        return "(?<%s>%s)".formatted(name, defaultValue == null ? MATCH_ALL : defaultValue);
    }

    @Override
    public boolean equals(Object object) {
        if (object == null || getClass() != object.getClass())
            return false;

        PathPattern that = (PathPattern) object;

        return Objects.equals(pattern, that.pattern);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(pattern);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [" + expression + "]";
    }

    /**
     * 📌 Internal structure describing a route parameter.
     *
     * @param name     parameter name
     * @param optional whether it's optional (not implemented yet)
     */
    record Parameter(String name, boolean optional) {
    }
}
