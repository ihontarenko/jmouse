package org.jmouse.web.mvc;

import org.jmouse.web.mvc.PathContainer.Element;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Objects.requireNonNull;

/**
 * 🧭 Minimal route matched implementation that plugs into {@link RoutePath}.
 *
 * <p>Supported syntax:</p>
 * <ul>
 *   <li>📜 Literals: {@code /api/users}</li>
 *   <li>❓ {@code ?} — exactly one char inside a segment</li>
 *   <li>⭐ {@code *} — zero or more chars inside a segment</li>
 *   <li>🔑 {@code {name}} — capture exactly one segment to a variable</li>
 *   <li>🔑 {@code {name:\w+}} — capture one segment constrained by regex</li>
 *   <li>🌌 {@code **} — match the rest of the path (must be last)</li>
 *   <li>🌌 {@code {*name}} — capture the rest of the path into a variable (must be last)</li>
 * </ul>
 *
 * <p>Not implemented (v1): matrix params, custom separators, case-insensitive mode.</p>
 */
public class SimplePathPattern implements RoutePath {

    private final String            raw;
    private final List<PathSegment> parts;

    private SimplePathPattern(String raw, List<PathSegment> parts) {
        this.raw = requireNonNull(raw);
        this.parts = List.copyOf(parts);
    }

    /**
     * @return raw matched string as defined by user
     */
    @Override
    public String raw() {
        return raw;
    }

    /**
     * ✅ Check if the given path matches this matched.
     */
    @Override
    public boolean matches(String path) {
        return doMatch(path, false) != null;
    }

    /**
     * 🎯 Perform a full match and return {@link RouteMatch}.
     */
    @Override
    public RouteMatch match(String path) {
        return doMatch(path, true);
    }

    /**
     * @return matched kind marker
     */
    @Override
    public Kind kind() {
        return Kind.COMBINED;
    }

    /**
     * ✂️ Extract the path substring starting from the first
     * non-literal/wildcard segment.
     */
    @Override
    public String extractPath(String path) {
        final List<Element> elements   = RoutePath.split(path);
        int                 nonLiteral = nonLiteralIndex(parts);

        if (elements.isEmpty() || nonLiteral < 0 || elements.size() <= nonLiteral) {
            return "";
        }

        return RoutePath.joinElements(elements, nonLiteral);
    }

    /**
     * 🔑 Extract variables from path.
     *
     * @return variable map or empty if no match
     */
    @Override
    public Map<String, Object> extractVariables(String path) {
        RouteMatch match = doMatch(path, false);

        if (match == null) {
            return Collections.emptyMap();
        }

        return match.variables();
    }

    /**
     * 📥 Parse a raw matched string into {@link SimplePathPattern}.
     */
    public static RoutePath parse(String pattern) {
        return new Parser().parse(pattern);
    }

    private RouteMatch doMatch(String path, boolean needExtracted) {
        final List<Element> elements        = RoutePath.split(path);
        int                 i               = 0;
        int                 j               = 0;
        Map<String, Object> variables       = new LinkedHashMap<>();
        int                 nonLiteralIndex = nonLiteralIndex(parts);

        while (i < parts.size()) {
            PathSegment part = parts.get(i);

            if (part instanceof PathSegment.CaptureAll captureAll) {
                String remainder = RoutePath.joinElements(elements, j);

                if (captureAll.name != null) {
                    variables.put(captureAll.name, remainder);
                }

                j = elements.size();
                break;
            }

            if (j >= elements.size()) {
                return null;
            }

            Element element = elements.get(j);
            if (!part.matches(element.value())) {
                return null;
            }

            if (part instanceof PathSegment.CaptureOne captureOne) {
                bindMatrixVariables(captureOne.name, element, variables);
                variables.put(captureOne.name, element.value());
            } else if (part instanceof PathSegment.Template template) {
                Map<String, String> captured = template.extract(element.value());
                if (!captured.isEmpty()) {
                    String bindTo = captured.keySet().iterator().next();
                    bindMatrixVariables(bindTo, element, variables);
                    variables.putAll(captured);
                }
            }

            i++; j++;
        }

        if (j != elements.size()) {
            return null;
        }

        String extracted = "";

        if (needExtracted && nonLiteralIndex >= 0 && elements.size() > nonLiteralIndex) {
            extracted = RoutePath.joinElements(elements, nonLiteralIndex);
        }

        return new RouteMatch(path, extracted, variables);
    }

    private static void bindMatrixVariables(String name, Element element, Map<String, Object> variables) {
        if (element instanceof PathContainer.PathElement pathElement) {
            Map<String, List<String>> matrixVariables = pathElement.parameters();
            if (matrixVariables != null && !matrixVariables.isEmpty()) {
                variables.put(";%s".formatted(name), matrixVariables);
            }
        }
    }

    private static int nonLiteralIndex(List<PathSegment> parts) {
        int index = -1;
        int count = 0;

        for (PathSegment part : parts) {
            if (!(part instanceof PathSegment.Literal literal) || containsWildcards(literal.toString())) {
                index = count;
                break;
            }
            count++;
        }

        return index;
    }

    private static boolean containsWildcards(String segment) {
        return segment.indexOf('*') != -1 || segment.indexOf('?') != -1;
    }

    /**
     * 🛠 Parser for {@link SimplePathPattern}.
     */
    public static final class Parser {

        /**
         * Parse a raw matched into {@link SimplePathPattern}.
         */
        public SimplePathPattern parse(String pattern) {
            PathContainer container = SimplePathContainer.parse(pattern);
            List<Element> elements  = container.elements(PathContainer.PathElement.class);

            if (elements.isEmpty()) {
                return new SimplePathPattern(pattern, List.of());
            }

            return new SimplePathPattern(pattern, getParts(pattern, elements));
        }

        private List<PathSegment> getParts(String pattern, List<Element> elements) {
            List<PathSegment> parts = new ArrayList<>(elements.size());

            for (int index = 0; index < elements.size(); index++) {
                Element     element = elements.get(index);
                PathSegment part    = parseSegment(element);

                if (part instanceof PathSegment.CaptureAll && index != elements.size() - 1) {
                    throw new IllegalArgumentException("** OR {*name} MUST BE THE LAST SEGMENT: %s".formatted(pattern));
                }

                parts.add(part);
            }
            return parts;
        }

        private PathSegment parseSegment(Element element) {
            String segment = element.value();

            if ("**".equals(segment)) {
                return new PathSegment.CaptureAll(null);
            }

            if (segment.startsWith("{*") && segment.endsWith("}")) {
                String name = segment.substring(2, segment.length() - 1);
                return new PathSegment.CaptureAll(name);
            }

            if ("*".equals(segment)) {
                return new PathSegment.Wildcard();
            }

            if ("?".equals(segment)) {
                return new PathSegment.SingleCharacter();
            }

            if (segment.startsWith("{") && segment.endsWith("}")) {
                // Supports {name} and {name:regex}
                String inside = segment.substring(1, segment.length() - 1);
                int    colon  = segment.indexOf(':');

                if (colon < 0) {
                    return new PathSegment.CaptureOne(inside);
                }

                String name  = inside.substring(0, colon - 1);
                String regex = inside.substring(colon);

                return new PathSegment.CaptureOne(name, Pattern.compile("^(?:%s)$".formatted(regex)));
            }

            if (segment.indexOf('{') >= 0 && segment.indexOf('}') > segment.indexOf('{')) {
                return PathSegment.Template.forSegment(segment);
            }

            return new PathSegment.Literal(segment);
        }

    }

    /**
     * 🔹 Segment types for matched matching.
     */
    sealed interface PathSegment permits
            PathSegment.Literal,
            PathSegment.Wildcard,
            PathSegment.Template,
            PathSegment.SingleCharacter,
            PathSegment.CaptureOne,
            PathSegment.CaptureAll {

        boolean matches(String segment);

        /**
         * Mixed literal-with-variables segment, e.g. "ID_{id:\d+}" or "x-{a}-{b:\w+}".
         */
        final class Template implements PathSegment {

            private final String       raw;
            private final Pattern      expression;
            private final List<String> names;

            private Template(String raw, Pattern expression, List<String> names) {
                this.raw = raw;
                this.expression = expression;
                this.names = names;
            }

            static Template forSegment(String segment) {
                StringBuilder builder = new StringBuilder("^(?:");
                List<String>  names   = new ArrayList<>();
                int           i       = 0;
                int           size    = segment.length();

                while (i < size) {
                    char character = segment.charAt(i);

                    if (character == '{') {
                        int    close      = segment.indexOf('}', i + 1);
                        String inside     = segment.substring(i + 1, close);
                        int    colon      = inside.indexOf(':');
                        String name       = (colon < 0) ? inside : inside.substring(0, colon);
                        String expression = (colon < 0) ? "[^/]+" : inside.substring(colon + 1);

                        names.add(name);
                        builder.append('(').append(expression).append(')');

                        i = close + 1;

                        continue;
                    }

                    toSegmentExpression(character, builder);

                    i++;
                }

                builder.append(")$");

                return new Template(segment, Pattern.compile(builder.toString()), names);
            }

            @Override
            public boolean matches(String segment) {
                return expression.matcher(segment).matches();
            }

            public Map<String, String> extract(String segment) {
                Matcher matcher = expression.matcher(segment);

                if (!matcher.matches()) {
                    return Collections.emptyMap();
                }

                Map<String, String> variables = new LinkedHashMap<>();

                for (int index = 1; index <= matcher.groupCount() && index <= names.size(); index++) {
                    variables.put(names.get(index - 1), matcher.group(index));
                }

                return variables;
            }

            @Override
            public String toString() {
                return raw;
            }
        }


        /**
         * 📜 Literal segment, possibly with wildcards (*, ?).
         */
        final class Literal implements PathSegment {

            private final String  raw;
            private final Pattern regex;

            Literal(String raw) {
                this.raw = requireNonNull(raw);
                this.regex = Pattern.compile(toSegmentExpression(raw));
            }

            public boolean matches(String segment) {
                return regex.matcher(segment).matches();
            }

            public String toString() {
                return raw;
            }

        }

        /**
         * ⭐ Entire segment is {@code *} (match any).
         */
        final class Wildcard implements PathSegment {

            public boolean matches(String segment) {
                return true;
            }

            public String toString() {
                return "*";
            }

        }

        /**
         * ❓ Entire segment is {@code ?} (match exactly one char).
         */
        final class SingleCharacter implements PathSegment {

            public boolean matches(String segment) {
                return segment.length() == 1;
            }

            public String toString() {
                return "?";
            }

        }

        /**
         * 🔑 Capture one segment into a variable (optionally regex-constrained).
         */
        final class CaptureOne implements PathSegment {

            final String  name;
            final Pattern constraint; // may be null => unconstrained

            CaptureOne(String name) {
                this(name, null);
            }

            CaptureOne(String name, Pattern constraint) {
                this.name = requireNonNull(name);
                this.constraint = constraint; // may be null
            }

            public boolean matches(String segment) {
                return constraint == null || constraint.matcher(segment).matches();
            }

            public String toString() {
                return constraint == null ? "{%s}".formatted(name) : "{%s:%s}".formatted(name, constraint.pattern());
            }

        }

        /**
         * 🌌 Capture all remaining segments (must be last).
         */
        final class CaptureAll implements PathSegment {

            final String name; // null for plain **

            CaptureAll(String name) {
                this.name = name;
            }

            public boolean matches(String segment) {
                return true; /* never called in practice */
            }

            public String toString() {
                return name == null ? "**" : "{*%s}".formatted(name);
            }

        }

        private static String toSegmentExpression(String raw) {
            StringBuilder builder = new StringBuilder("^(?:");

            for (int i = 0; i < raw.length(); i++) {
                toSegmentExpression(raw.charAt(i), builder);
            }

            return builder.append(")$").toString();
        }

        private static void toSegmentExpression(char character, StringBuilder builder) {
            if (character == '*') {
                builder.append(".*");
            } else if (character == '?') {
                builder.append('.');
            } else if (".[]{}()^$|+\\".indexOf(character) >= 0) {
                builder.append('\\').append(character);
            } else {
                builder.append(character);
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();

        for (PathSegment part : parts) {
            builder
                    .append(part.getClass().getSimpleName())
                    .append("[")
                    .append(part)
                    .append("]/");
        }

        return builder.toString();
    }

}
