package org.jmouse.web.match;

/**
 * 🏭 Factory for creating {@link PathPattern} instances.
 *
 * <p>Chooses appropriate implementation based on path expression:</p>
 * <ul>
 *   <li>🟢 {@link AntPattern} — for ant-style wildcards (e.g. {@code /assets/**})</li>
 *   <li>🟢 {@link RegexpPathPattern} — for URI templates with variables (e.g. {@code /users/{id}})</li>
 * </ul>
 */
public final class PathPatternCompiler {

    private PathPatternCompiler() {
        // 🚫 utility class, no instantiation
    }

    /**
     * 🔧 Create a {@link PathPattern} for the given path expression.
     *
     * @param path raw route path expression
     * @return matching {@link PathPattern} implementation
     */
    public static PathPattern compile(String path) {
        if (path.contains("{*")) {
            return SimplePathPattern.parse(path);
        } else if (path.contains("{") && path.contains("}")) {
            return new RegexpPathPattern(path);
        } else {
            return new AntPattern(path);
        }
    }
}
