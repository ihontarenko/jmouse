package org.jmouse.web.mvc.resource;

import org.jmouse.core.io.Resource;
import org.jmouse.core.matcher.ant.AntMatcher;
import org.jmouse.web.http.request.PathQuery;

import java.util.Objects;

/**
 * 📌 Fixed versioning strategy for static resources.
 *
 * <p>Prepends a fixed version segment to resource paths
 * (e.g. {@code /v1/js/app.js}).</p>
 */
public class FixedVersionStrategy implements VersionStrategy {

    /**
     * 🔎 Ant matcher for supported resource paths.
     */
    private final AntMatcher matcher;

    /**
     * 🏷️ Fixed version identifier.
     */
    private final String version;

    /**
     * 🏗️ Create a new strategy with an ant pattern string.
     *
     * @param matcher ant-style matcher expression
     * @param version fixed version to apply
     */
    public FixedVersionStrategy(String matcher, String version) {
        this(new AntMatcher(matcher), version);
    }

    /**
     * 🏗️ Create a new strategy with an ant matcher.
     *
     * @param matcher precompiled {@link AntMatcher}
     * @param version fixed version to apply
     */
    public FixedVersionStrategy(AntMatcher matcher, String version) {
        this.matcher = matcher;
        this.version = version;
    }

    /**
     * 🧹 Normalize version string (trim, strip slashes).
     */
    private static String sanitizeVersion(String version) {
        String sanitized = version.trim();

        if (sanitized.startsWith("/")) {
            sanitized = sanitized.substring(1);
        }

        if (sanitized.endsWith("/")) {
            sanitized = sanitized.substring(0, sanitized.length() - 1);
        }

        return sanitized;
    }

    /**
     * 🔍 Extract versioned path information from a request.
     *
     * @param requestPath incoming path
     * @return {@link VersionPath} if matches and versioned, else {@code null}
     */
    @Override
    public VersionPath getVersion(String requestPath) {
        if (requestPath == null || !isSupports(requestPath)) {
            return null;
        }

        String    version   = sanitizeVersion(this.version);
        PathQuery pathQuery = PathQuery.ofPath(requestPath);
        String    path      = pathQuery.path();
        String    noLead    = path.startsWith("/") ? path.substring(1) : path;
        String    prefix    = version + "/";

        if (!noLead.startsWith(prefix)) {
            return null;
        }

        String simple = noLead.substring(prefix.length());

        return new VersionPath(simple, version);
    }

    /**
     * ✂️ Remove version prefix from a path.
     *
     * @param path    original path
     * @param version version string to strip
     * @return path without version, preserving query string
     */
    @Override
    public String removeVersion(String path, String version) {
        if (path == null) {
            return null;
        }

        PathQuery pathQuery   = PathQuery.ofPath(path);
        String    clearPath   = pathQuery.path();
        String    sanitized   = sanitizeVersion(version);
        String    prefix      = sanitized + "/";
        String    noLead      = clearPath.startsWith("/") ? clearPath.substring(1) : path;
        String    queryString = pathQuery.query();

        if (noLead.startsWith(prefix)) {
            return noLead.substring(prefix.length()) + (queryString == null ? "" : queryString);
        }

        return path;
    }

    /**
     * ➕ Insert version prefix into the given path.
     *
     * @param versionPath simple path + version
     * @return versioned path with query string preserved
     */
    @Override
    public String putVersion(VersionPath versionPath) {
        String    simple      = versionPath.simple();
        String    version     = sanitizeVersion(Objects.requireNonNullElse(versionPath.version(), this.version));
        PathQuery pathQuery   = PathQuery.ofPath(simple);
        String    result      = (simple.startsWith("/") ? "" : "/") + version + "/" + simple;
        String    queryString = pathQuery.query();

        result = result.replaceAll("/{2,}", "/");

        return result + (queryString == null ? "" : queryString);
    }

    /**
     * ⚡ Generation not supported for fixed strategy.
     *
     * @param resource resource reference
     * @throws UnsupportedOperationException always
     */
    @Override
    public VersionPath generateVersion(Resource resource) {
        throw new UnsupportedOperationException();
    }

    /**
     * ✅ Check if this strategy applies to a request path.
     *
     * @param requestPath candidate path
     * @return {@code true} if matches pattern
     */
    @Override
    public boolean isSupports(String requestPath) {
        return matcher.matches(requestPath);
    }
}
