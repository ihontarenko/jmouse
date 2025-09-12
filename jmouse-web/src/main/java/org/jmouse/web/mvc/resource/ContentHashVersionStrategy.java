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
 * 📦 Content-hash versioning strategy: file name contains "/{hex}/app.js" before extension.
 *
 * <p>Examples:</p>
 * <ul>
 *   <li>{@code assets/1fc92ffa/app.js}</li>
 *   <li>{@code assets/9f8e7d6c/styles.min.css}</li>
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
            "^(?<path>.*/)?(?<version>[A-Fa-f0-9]{6,64})/(?<name>[^/]+?)(?<suffix>(?:\\\\.[^./]+)*)$");

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
        this.hexLength = Math.max(6, Math.min(hexLength, 64));
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
     * @param path resource path that may contain version
     * @param version      version string to remove
     * @return path without version part, or unchanged if no match
     */
    @Override
    public String removeVersion(String path, String version) {
        if (path == null || version == null || version.isEmpty()) {
            return path;
        }

        PathQuery pathQuery = PathQuery.ofPath(path);
        String    pure      = pathQuery.path();
        String    query     = pathQuery.query();
        int       index     = pure.indexOf(version);

        if (index < 0) {
            return path;
        }

        String after  = pure.substring(0, pure.indexOf(version));
        String before = pure.substring(pure.indexOf(version) + version.length() + 1);

        query = (query == null ? "" : "?%s".formatted(query));

        return "%s%s%s".formatted(after, before, query);
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
        int       slash     = pure.lastIndexOf('/');
        String    head      = pure.substring(0, slash);
        String    tail      = pure.substring(slash);

        query = (query == null ? "" : "?%s".formatted(query));

        return "%s/%s%s%s".formatted(head, version, tail, query);
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
