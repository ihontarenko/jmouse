package org.jmouse.web.mvc;

import org.jmouse.core.matcher.ant.AntMatcher;
import org.jmouse.web.mvc.PathContainer.Element;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 🐜 Ant-style route pattern implementation.
 *
 * <p>Examples:
 * <ul>
 *   <li>{@code /assets/**}</li>
 *   <li>{@code /img/*.png}</li>
 * </ul>
 * </p>
 */
public class AntPattern implements RoutePath {

    /** 📍 Raw path expression (ant-style). */
    private final String path;

    /** 🔎 Underlying ant matcher for path evaluation. */
    private final AntMatcher antMatcher;

    /**
     * 🏗️ Create a new ant path pattern.
     *
     * @param path ant-style path expression
     */
    public AntPattern(String path) {
        this.path = path;
        this.antMatcher = new AntMatcher(path);
    }

    /**
     * @return the raw ant-style path expression
     */
    @Override
    public String raw() {
        return path;
    }

    /**
     * ✅ Check if given path matches this pattern.
     *
     * @param path request path
     * @return {@code true} if matches
     */
    @Override
    public boolean matches(String path) {
        return antMatcher.matches(path);
    }

    /**
     * 🔗 Attempt to match and produce a {@link RouteMatch}.
     *
     * <p>Currently returns an empty parameter map.</p>
     *
     * @param path request path
     * @return route match object
     */
    @Override
    public RouteMatch match(String path) {
        return new RouteMatch(path, Map.of());
    }

    /**
     * @return the kind of this route path ({@link Kind#ANT})
     */
    @Override
    public Kind kind() {
        return Kind.ANT;
    }

    @Override
    public String extractPath(String path) {
        List<Element> elements   = RoutePath.split(path);
        int           nonLiteral = dynamicIndex(elements);

        if (nonLiteral == -1 || nonLiteral == elements.size() - 1) {
            return "";
        }

        return RoutePath.joinElements(elements, nonLiteral);
    }

    public static void main(String[] args) {
        AntPattern antPattern = new AntPattern("/assets/**");

        System.out.println(antPattern.extractPath("/assets/css/jmouse.css"));
    }

    public AntMatcher getAntMatcher() {
        return antMatcher;
    }

    /**
     * Index of the first non-literal (pattern-based) segment in the PATTERN.
     */
    private static int dynamicIndex(List<Element> patternElements) {
        int index = 0;

        for (Element element : patternElements) {
            String segment = element.value();

            if (segment.contains(AntMatcher.ANY_MULTI_SEGMENT)
                    || segment.contains(AntMatcher.ANY_CHARACTER)
                    || segment.contains(AntMatcher.ANY_SINGLE_SEGMENT)) {
                return index;
            }

            index++;
        }

        return -1;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof AntPattern that)) {
            return false;
        }

        return Objects.equals(path, that.path);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(path);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [" + raw() + "]";
    }

}
