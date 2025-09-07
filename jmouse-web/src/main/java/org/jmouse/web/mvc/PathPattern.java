package org.jmouse.web.mvc;

import org.jmouse.core.PlaceholderReplacer;
import org.jmouse.core.StandardPlaceholderReplacer;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 🧭 Parses path patterns like <code>/user/{id:\d+}/status/{status}</code> into regular expressions
 * and extracts matched parameters into {@link RouteMatch}.
 *
 * <p>💡 Example:
 * <pre>{@code
 * PathPattern pattern = new PathPattern("/user/{id:int}/status/{status}");
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
public final class PathPattern implements RoutePath {

    /** 🔠 Type name for boolean params */
    public static final String BOOLEAN_NAME = "boolean";

    /** 🔠 Type name for boolean params (alias) */
    public static final String BOOL_NAME = "bool";

    /** 🔢 Type name for integer params */
    public static final String INT_NAME = "int";

    /** 🔁 Placeholder replacer: `{name:pattern}` */
    public static final PlaceholderReplacer REPLACER = new StandardPlaceholderReplacer("{", "}", ":");

    /** 📦 Default pattern for untyped params */
    public static final String MATCH_ALL = "[^/]+";

    /** ✅ Pattern for boolean values */
    public static final String MATCH_BOOLEAN = "true|false";

    /** 🔢 Pattern for integers */
    public static final String MATCH_INT = "\\d+";

    /** 🔢 Type name for custom regexp */
    public static final String CUSTOM_NAME = "custom";

    private final String          pattern;
    private final Pattern         expression;
    private final List<Parameter> parameters = new ArrayList<>();

    /**
     * 🔧 Constructs a {@code PathPattern} by compiling the given view.
     *
     * @param pattern route pattern, e.g., {@code /user/{id:\d+}/status/{status}}
     */
    public PathPattern(String pattern) {
        this.pattern = pattern;
        this.expression = compile(pattern);
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

        return Objects.equals(pattern, ((PathPattern) object).pattern);
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
