package org.jmouse.web.mvc.resource;

import org.jmouse.core.io.Resource;
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
     * 🏷️ Fixed version identifier.
     */
    private final String version;

    /**
     * 🏗️ Create a new strategy with an ant matched string.
     *
     * @param version fixed version to apply
     */
    public FixedVersionStrategy(String version) {
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
     * @return {@link PathVersion} if matches and versioned, else {@code null}
     */
    @Override
    public PathVersion getVersion(String requestPath) {
        if (requestPath == null) {
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

        return new PathVersion(simple, version);
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
    public String putVersion(PathVersion versionPath) {
        String    simple      = versionPath.simple();
        String    version     = sanitizeVersion(Objects.requireNonNullElse(versionPath.version(), this.version));
        PathQuery pathQuery   = PathQuery.ofPath(simple);
        String    result      = version + "/" + simple;
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
    public String generateVersion(Resource resource) {
        return version;
    }

    /**
     * ✅ Validate whether the given version matches the provided resource.
     *
     * <p>Implementations may check checksum, timestamp, hash,
     * or other resource metadata to ensure the version is correct.</p>
     *
     * @param resource the resource to verify
     * @param version  the extracted version string
     * @return {@code true} if version is valid for the resource, {@code false} otherwise
     */
    @Override
    public boolean validateVersion(Resource resource, String version) {
        return version.startsWith(this.version);
    }
}
