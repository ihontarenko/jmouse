package org.jmouse.web.mvc.resource;

import org.jmouse.web.http.PathQuery;
import org.jmouse.web.http.QueryParser;
import org.jmouse.web.http.QueryParser.QueryParameter;
import org.jmouse.web.http.QueryParser.QueryString;

/**
 * ❓ Query-parameter based {@link VersionStrategy}.
 *
 * <p>Encodes resource versions as query parameters, e.g.:</p>
 * <ul>
 *   <li>{@code /style.css?v=12345}</li>
 *   <li>{@code /app.js?v=abcdef}</li>
 * </ul>
 *
 * <p>💡 Useful for CDNs and browsers that cache aggressively by path.</p>
 */
public class QueryParameterVersionStrategy extends AbstractVersionStrategy {

    /**
     * Default query parameter name ("v").
     */
    public static final String DEFAULT_PARAMETER = "v";

    /**
     * 🏷️ Query parameter name (e.g. "v").
     */
    private final String parameter;

    /**
     * 🏗️ Create a new strategy with custom parameter, algorithm, and length.
     *
     * @param parameter query parameter name
     * @param algorithm digest algorithm (e.g. SHA-256)
     * @param length    hex length for normalization
     */
    public QueryParameterVersionStrategy(String parameter, String algorithm, int length) {
        super(algorithm, length);
        this.parameter = parameter;
    }

    /**
     * 🏗️ Create a new strategy with custom parameter and defaults for algorithm/length.
     *
     * @param parameter query parameter name
     */
    public QueryParameterVersionStrategy(String parameter) {
        this(parameter, DEFAULT_ALGORITHM, DEFAULT_LENGTH);
    }

    /**
     * ⚙️ Create a new strategy using default parameter ("v"),
     * algorithm ({@link #DEFAULT_ALGORITHM}), and length ({@link #DEFAULT_LENGTH}).
     */
    public QueryParameterVersionStrategy() {
        this(DEFAULT_PARAMETER);
    }

    /**
     * 🔍 Extract the version from the query parameter.
     *
     * @param path request path with query string
     * @return {@link PathVersion} or {@code null} if parameter not present
     */
    @Override
    public PathVersion getVersion(String path) {
        if (path == null) {
            return null;
        }

        PathQuery      pathQuery = PathQuery.ofPath(path);
        String         base      = pathQuery.path();
        QueryString    query     = getQueryString(path);
        QueryParameter parameter = query.getParameter(this.parameter);

        if (parameter == null) {
            return null;
        }

        String without = query.toQueryString(this.parameter);
        String simple  = base + (without.isEmpty() ? "" : "?" + without);

        return new PathVersion(simple, parameter.value());
    }

    /**
     * ✂️ Remove version parameter if it matches the given value.
     *
     * @param path    original path with query
     * @param version version to remove
     * @return path without version parameter, or {@code null} if not matched
     */
    @Override
    public String removeVersion(String path, String version) {
        PathVersion pathVersion = getVersion(path);

        if (pathVersion == null || !pathVersion.version().equals(version)) {
            return null;
        }

        return pathVersion.simple();
    }

    /**
     * ➕ Insert version parameter into the given path.
     *
     * @param versionPath base path and version
     * @return path with {@code ?v=...} query parameter
     */
    @Override
    public String putVersion(PathVersion versionPath) {
        if (versionPath == null || versionPath.simple() == null) {
            return null;
        }

        String      path  = versionPath.simple();
        QueryString query = getQueryString(path);

        query.addParameter(this.parameter, versionPath.version());

        return path + "?" + query.toQueryString();
    }

    /**
     * 🛠️ Extract the query string from a path and parse it.
     *
     * @param path full path with optional query
     * @return parsed {@link QueryString} (never {@code null})
     */
    private QueryString getQueryString(String path) {
        if (path == null) {
            return new QueryString();
        }

        PathQuery pathQuery   = PathQuery.ofPath(path);
        String    queryString = pathQuery.query();

        if (queryString != null) {
            return QueryParser.parse(queryString.startsWith("?") ? queryString.substring(1) : queryString);
        }

        return new QueryString();
    }
}
