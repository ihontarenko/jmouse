package org.jmouse.web.mvc.resource;

import org.jmouse.web.http.request.PathQuery;

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
public final class ContentHashVersionStrategy extends AbstractVersionStrategy {

    /**
     * Regex pattern for matching file names with hash suffix.
     * <pre>{@code path/name-{hex}[.<suffix>...]}</pre>
     */
    private static final Pattern NAME_WITH_HASH = Pattern.compile(
            "^(?<path>.*/)?(?<version>[A-Fa-f0-9]{6,64})/(?<name>[^/]+?)(?<suffix>(?:\\\\.[^./]+)*)$");

    /**
     * 🏗️ Create a new strategy with the given algorithm and hash length.
     *
     * @param algorithm digest algorithm name (e.g. {@code MD5}, {@code SHA-1}, {@code SHA-256})
     * @param hexLength expected hex length (e.g. 32 for MD5, 40 for SHA-1, 64 for SHA-256),
     *                  or {@code 0} to disable normalization
     */
    public ContentHashVersionStrategy(String algorithm, int hexLength) {
        super(algorithm, hexLength);
    }

    /**
     * ⚙️ Create a new strategy using default algorithm and length.
     *
     * <p>Defaults to {@code SHA-256} with a 64-character hex digest
     * (see {@link #DEFAULT_ALGORITHM} and {@link #DEFAULT_LENGTH}).</p>
     */
    public ContentHashVersionStrategy() {
        this(DEFAULT_ALGORITHM, DEFAULT_LENGTH);
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
        version = toLength(version, length);

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


}
