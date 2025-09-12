package org.jmouse.web.mvc.resource;

import org.jmouse.core.io.Resource;

/**
 * 🏷️ Strategy interface for resource path versioning (using {@link PathVersion}).
 *
 * <p>Defines operations to:</p>
 * <ul>
 *   <li>🔍 Extract a version from a request path</li>
 *   <li>✂️ Remove a version prefix</li>
 *   <li>➕ Insert a version prefix</li>
 *   <li>⚡ Generate a version from resource metadata</li>
 *   <li>✅ Validate a version against resource content</li>
 * </ul>
 */
public interface VersionStrategy {

    /**
     * 🔍 Try to extract a version from the given path.
     *
     * @param path original request path (may contain version prefix)
     * @return {@link PathVersion} with simple path and version, or {@code null} if not versioned
     */
    PathVersion getVersion(String path);

    /**
     * ✂️ Remove version prefix from the given path.
     *
     * @param path    original path
     * @param version version string to remove
     * @return path without version prefix (or unchanged if not applicable)
     */
    String removeVersion(String path, String version);

    /**
     * ➕ Insert version prefix into the given path.
     *
     * @param versionPath container with simple path and version
     * @return full versioned path string (query string preserved if present)
     */
    String putVersion(PathVersion versionPath);

    /**
     * ⚡ Generate a version dynamically based on resource metadata.
     *
     * @param resource resource to analyze
     * @return generated {@link String} version
     * @throws UnsupportedOperationException if not supported
     */
    String generateVersion(Resource resource);

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
    boolean validateVersion(Resource resource, String version);
}
