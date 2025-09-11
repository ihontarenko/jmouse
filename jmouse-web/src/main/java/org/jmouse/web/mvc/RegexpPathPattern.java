package org.jmouse.web.mvc;

import org.jmouse.core.PlaceholderReplacer;
import org.jmouse.core.StandardPlaceholderReplacer;
import org.jmouse.web.mvc.PathContainer.Element;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.jmouse.web.mvc.SimplePathContainer.parse;

/**
 * 🧭 Parses path patterns like <code>/user/{id:\d+}/status/{status}</code> into regular expressions
 * and extracts matched parameters into {@link RouteMatch}.
 *
 * <p>💡 Example:
 * <pre>{@code
 * RegexpPathPattern pattern = new RegexpPathPattern("/user/{id:int}/status/{status}");
 * RoutePath route = pattern.parse("/user/42/status/ACTIVE");
 * int id = route.getInt("id", -1);
 * String status = route.getString("status", "UNKNOWN");
 * }</pre>
 *
 * Supports:
 * <ul>
 *   <li>Named parameters</li>
 *   <li>Typed parameters: {@code int}, {@code boolean}</li>
 *   <li>Custom regex per parameter</li>
 * </ul>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class RegexpPathPattern implements RoutePath {

    /**
     * 🔠 Type name for boolean params
     */
    public static final String BOOLEAN_NAME = "boolean";

    /**
     * 🔠 Type name for boolean params (alias)
     */
    public static final String BOOL_NAME = "bool";

    /**
     * 🔢 Type name for integer params
     */
    public static final String INT_NAME = "int";

    /**
     * 🔁 Placeholder replacer: `{name:pattern}`
     */
    public static final PlaceholderReplacer REPLACER = new StandardPlaceholderReplacer("{", "}", ":");

    /**
     * 📦 Default pattern for untyped params
     */
    public static final String MATCH_ALL = "[^/]+";

    /**
     * ✅ Pattern for boolean values
     */
    public static final String MATCH_BOOLEAN = "true|false";

    /**
     * 🔢 Pattern for integers
     */
    public static final String MATCH_INT = "\\d+";

    /**
     * 🔢 Type name for custom regexp
     */
    public static final String CUSTOM_NAME = "custom";

    private final String          pattern;
    private final Pattern         expression;
    private final List<Element>   elements;
    private final List<Parameter> parameters = new ArrayList<>();

    /**
     * 🔧 Constructs a {@code RegexpPathPattern} by compiling the given view.
     *
     * @param pattern route pattern, e.g., {@code /user/{id:\d+}/status/{status}}
     */
    public RegexpPathPattern(String pattern) {
        this.pattern = pattern;
        this.expression = compile(pattern);
        this.elements = RoutePath.split(pattern);
    }

    @Override
    public String raw() {
        return pattern;
    }

    /**
     * 🎯 Parses the input path and returns extracted typed parameters.
     *
     * @param input URI path, e.g., {@code /user/42/status/ACTIVE}
     * @return {@link RouteMatch} with extracted values
     */
    @Override
    public RouteMatch match(String input) {
        Matcher             matcher   = expression.matcher(input);
        Map<String, Object> variables = new LinkedHashMap<>(8);

        if (matcher.matches()) {
            for (Parameter parameter : parameters) {
                Object raw = matcher.group(parameter.name());

                Object variable = switch (parameter.type().name()) {
                    case INT_NAME                   -> Integer.parseInt(raw.toString());
                    case BOOLEAN_NAME, BOOL_NAME    -> Boolean.parseBoolean(raw.toString());
                    default                         -> raw;
                };

                variables.put(parameter.name(), variable);
            }
        }

        return new RouteMatch(input, variables);
    }

    @Override
    public Kind kind() {
        return Kind.TEMPLATE;
    }

    /**
     * 🔍 Checks if the input matches the compiled pattern.
     *
     * @param input input URI
     * @return true if matched
     */
    @Override
    public boolean matches(String input) {
        return expression.matcher(input).matches();
    }

    /**
     * ✂️ Extract the static or simplified path representation.
     *
     * <p>The exact behavior depends on the {@link RoutePath} implementation
     * (e.g. stripping wildcards, normalizing templates).</p>
     *
     * @param path request path
     * @return simplified or extracted path
     */
    @Override
    public String extractPath(String path) {
        // Normalize request into path-elements
        List<Element> elements = RoutePath.split(path);

        if (elements.isEmpty()) {
            return "";
        }

        // Determine the first dynamic segment in the PATTERN
        List<Element> patterns   = getElements();
        int           nonLiteral = dynamicIndex(patterns);

        // If no dynamic part, or path is shorter than the literal prefix — nothing to extract
        if (nonLiteral < 0 || elements.size() <= nonLiteral) {
            return "";
        }

        // Return the portion of the path starting from the first pattern-based segment
        return RoutePath.joinElements(elements, nonLiteral);
    }

    /**
     * Index of the first non-literal (pattern-based) segment in the PATTERN.
     */
    private static int dynamicIndex(List<Element> patternElements) {
        int index = 0;

        for (Element element : patternElements) {
            String segment = element.value();

            if ((segment.indexOf('{') >= 0 && segment.indexOf('}') > segment.indexOf('{'))) {
                return index;
            }

            index++;
        }

        return -1;
    }

    /**
     * 🔑 Extract template variables from the given path.
     *
     * <p>Default implementation returns an empty map. Implementations
     * may override to provide actual variable extraction.</p>
     *
     * @param path request path
     * @return map of variable names to values (never {@code null})
     */
    @Override
    public Map<String, Object> extractVariables(String path) {
        return match(path).variables();
    }

    public List<Element> getElements() {
        return elements;
    }

    /**
     * 🔨 Compiles the route pattern into a regex with named capture groups.
     */
    private Pattern compile(String pattern) {
        return Pattern.compile("^%s$".formatted(REPLACER.replace(pattern, this::replace)));
    }

    /**
     * 🔧 Replaces a placeholder with a named capture group.
     *
     * @param name    parameter name
     * @param pattern type or regex
     * @return regex group string
     */
    private String replace(String name, String pattern) {
        Parameter.Type type = getParameterType(pattern);
        parameters.add(new Parameter(name, name.contains("?"), type));
        return "(?<%s>%s)".formatted(name, type.pattern());
    }

    /**
     * 🧠 Infers parameter type by placeholder pattern.
     */
    private Parameter.Type getParameterType(String pattern) {
        if (pattern == null) {
            pattern = MATCH_ALL;
        }

        return switch (pattern) {
            case INT_NAME                   -> new Parameter.Type(Integer.class, pattern, MATCH_INT);
            case BOOLEAN_NAME, BOOL_NAME    -> new Parameter.Type(Boolean.class, pattern, MATCH_BOOLEAN);
            default                         -> new Parameter.Type(Object.class, CUSTOM_NAME, pattern);
        };
    }

    @Override
    public boolean equals(Object object) {
        if (object == null || getClass() != object.getClass())
            return false;

        return Objects.equals(pattern, ((RegexpPathPattern) object).pattern);
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
     * @param name     name of parameter
     * @param optional currently unused, reserved for future
     * @param type     parameter type definition
     */
    record Parameter(String name, boolean optional, Type type) {

        /**
         * 🧩 Describes parameter type and regex used.
         *
         * @param type    Java type
         * @param name    symbolic type name (e.g. "int", "boolean")
         * @param pattern regex used for matching
         */
        record Type(Class<?> type, String name, String pattern) {
        }
    }
}
