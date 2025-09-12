package org.jmouse.web.mvc.resource;

import org.jmouse.core.io.Resource;
import org.jmouse.web.http.request.PathQuery;

import java.io.InputStream;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Objects.requireNonNull;
import static org.jmouse.util.StringHelper.*;

/**
 * 📦 Content-hash versioning strategy: file name contains "-{hex}" before extension.
 *
 * <p>Examples:</p>
 * <ul>
 *   <li>{@code app-0123abcd.js}</li>
 *   <li>{@code styles-9f8e7d6c.min.css}</li>
 * </ul>
 *
 * <p>Supports extracting, inserting, removing, validating, and generating
 * hash-based versions for static resources.</p>
 */
public final class ContentHashVersionStrategy implements VersionStrategy {

    /**
     * Regex pattern for matching file names with hash suffix.
     * <pre>{@code path/name-{hex}[.<suffix>...]}</pre>
     */
    private static final Pattern NAME_WITH_HASH = Pattern.compile(
            "^(?<path>.*/)?(?<name>[^/]+?)-(?<version>[A-Fa-f0-9]{6,64})(?<suffix>(?:\\.[^./]+)*)$");

    /**
     * Hash algorithm (e.g., MD5, SHA-1, SHA-256).
     */
    private final String algorithm;

    /**
     * Expected hex string length (e.g., 32 for MD5, 40 for SHA-1, 64 for SHA-256).
     */
    private final int hexLength;

    /**
     * 🏗️ Create a new strategy for the given algorithm and hash length.
     *
     * @param algorithm digest algorithm name (see {@link java.security.MessageDigest})
     * @param hexLength expected hex length, or {@code 0} for no normalization
     */
    public ContentHashVersionStrategy(String algorithm, int hexLength) {
        this.algorithm = requireNonNull(algorithm);
        this.hexLength = hexLength;
    }

    /**
     * 🔍 Extract a version from the given resource path if present.
     *
     * @param resource file path (may include version)
     * @return {@link PathVersion} with base path and version, or {@code null} if not versioned
     */
    @Override
    public PathVersion getVersion(String resource) {
        Matcher matcher = NAME_WITH_HASH.matcher(resource);

        if (!matcher.matches()) {
            return null;
        }

        String version = matcher.group("version");
        String path    = matcher.group("path");
        String name    = matcher.group("name");
        String suffix  = matcher.group("suffix");
        String base    = (path != null ? path : "") + name + suffix;

        // normalize to expected length
        if (hexLength > 0 && version.length() != hexLength) {
            version = version.substring(0, hexLength);
        }
        version = toLength(version, hexLength);

        return new PathVersion(base, version.toLowerCase(Locale.ROOT));
    }

    /**
     * ✂️ Remove a version suffix from the given path.
     *
     * @param resourcePath resource path that may contain version
     * @param version      version string to remove
     * @return path without version part, or unchanged if no match
     */
    @Override
    public String removeVersion(String resourcePath, String version) {
        if (resourcePath == null || version == null || version.isEmpty()) {
            return resourcePath;
        }

        PathQuery pathQuery = PathQuery.ofPath(resourcePath);
        String    pure      = pathQuery.path();
        String    query     = pathQuery.query();
        int       slash     = pure.lastIndexOf('/');
        int       dot       = pure.indexOf('.', Math.max(0, slash + 1));

        if (dot < 0) {
            // no suffix — remove trailing "-{version}" if present
            String suffix = "-" + version.toLowerCase(Locale.ROOT);

            if (pure.toLowerCase(Locale.ROOT).endsWith(suffix)) {
                return pure.substring(0, pure.length() - suffix.length()) + query;
            }

            return resourcePath;
        }

        String head  = pure.substring(0, dot); // .../name[-ver]
        String tail  = pure.substring(dot);    // ".min.js"
        String lower = head.toLowerCase(Locale.ROOT);
        String mark  = "-" + version.toLowerCase(Locale.ROOT);

        if (lower.endsWith(mark)) {
            return head.substring(0, head.length() - mark.length()) + tail + query;
        }

        // try prefix match if version length differs
        int idx = lower.lastIndexOf('-');
        if (idx >= 0 && dot > idx + 1) {
            String maybe = lower.substring(idx + 1);
            if (maybe.startsWith(version.toLowerCase(Locale.ROOT))) {
                return head.substring(0, idx) + tail + query;
            }
        }

        return resourcePath;
    }

    /**
     * ➕ Insert a version into the given path.
     *
     * @param versionPath simple path + version
     * @return path with version inserted before suffix
     */
    @Override
    public String putVersion(PathVersion versionPath) {
        String version      = versionPath.version();
        String resourcePath = versionPath.simple();

        if (resourcePath == null || resourcePath.isEmpty()) {
            return resourcePath;
        }

        PathQuery pathQuery = PathQuery.ofPath(resourcePath);
        String    pure      = pathQuery.path();
        String    query     = pathQuery.query();

        int slash = pure.lastIndexOf('/');
        int dot   = pure.indexOf('.', Math.max(0, slash + 1));

        if (dot < 0) {
            return pure + "-" + version + query;
        }

        String head = pure.substring(0, dot);
        String tail = pure.substring(dot);

        return head + "-" + version + tail + query;
    }

    /**
     * ⚡ Generate a version string from resource content using hash digest.
     *
     * @param resource the resource to analyze
     * @return hex string truncated/padded to configured {@code hexLength}
     * @throws IllegalStateException if digest computation fails
     */
    @Override
    public String generateVersion(Resource resource) {
        try (InputStream input = resource.getInputStream()) {
            String version = hex(digest(input, algorithm)).toLowerCase(Locale.ROOT);
            return toLength(version, hexLength);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to compute %s for resource: %s".formatted(algorithm, resource),
                                            exception);
        }
    }

    /**
     * ✅ Validate whether the given version matches the resource content.
     *
     * @param resource the resource to check
     * @param version  extracted version string
     * @return {@code true} if content hash starts with the version string
     */
    @Override
    public boolean validateVersion(Resource resource, String version) {
        try (InputStream stream = resource.getInputStream()) {
            String versionHash = hex(digest(stream, algorithm));
            return versionHash.startsWith(version.toLowerCase(Locale.ROOT));
        } catch (Exception exception) {
            return false;
        }
    }
}
