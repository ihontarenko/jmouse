package org.jmouse.web.mvc.resource;

import org.jmouse.core.io.Resource;

import java.io.InputStream;
import java.util.Locale;

import static org.jmouse.util.StringHelper.*;

/**
 * 🏗️ Base class for content-hash based {@link VersionStrategy} implementations.
 *
 * <p>Provides common logic for generating and validating version strings
 * using message digests (e.g. MD5, SHA-1, SHA-256).</p>
 *
 * <p>Subclasses define how versions are inserted or extracted
 * from resource paths, while this class handles the digest math.</p>
 */
public abstract class AbstractVersionStrategy implements VersionStrategy {

    /**
     * ⚙️ Default digest algorithm (SHA-256).
     */
    public static final String DEFAULT_ALGORITHM = "SHA-256";

    /**
     * 📏 Default hex length (16 chars, truncated SHA-256).
     */
    public static final int DEFAULT_LENGTH = 16;

    /**
     * 🔑 Hash algorithm (e.g. MD5, SHA-1, SHA-256).
     */
    protected final String algorithm;

    /**
     * 📐 Expected hex string length (e.g. 32 for MD5, 40 for SHA-1, 64 for SHA-256).
     */
    protected final int length;

    /**
     * 🏗️ Create a new version strategy with the given algorithm and length.
     *
     * @param algorithm digest algorithm to use
     * @param length    desired hex length (0 = no normalization)
     */
    protected AbstractVersionStrategy(String algorithm, int length) {
        this.algorithm = algorithm;
        this.length = length;
    }

    /**
     * ⚡ Generate a version string from resource content using hash digest.
     *
     * @param resource the resource to analyze
     * @return lowercase hex string, truncated/padded to {@code hexLength}
     * @throws IllegalStateException if digest computation fails
     */
    @Override
    public String generateVersion(Resource resource) {
        try (InputStream input = resource.getInputStream()) {
            String version = hex(digest(input, algorithm)).toLowerCase(Locale.ROOT);
            return toLength(version, length);
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Failed to compute %s for resource: %s".formatted(algorithm, resource), exception);
        }
    }

    /**
     * ✅ Validate whether the given version matches the resource content.
     *
     * <p>Checks if the resource's hash (using the configured algorithm)
     * starts with the provided version string.</p>
     *
     * @param resource the resource to check
     * @param version  extracted version string
     * @return {@code true} if hash matches, {@code false} otherwise
     */
    @Override
    public boolean validateVersion(Resource resource, String version) {
        if (version.length() < length) {
            return false;
        }

        try (InputStream stream = resource.getInputStream()) {
            String versionHash = hex(digest(stream, algorithm));
            return versionHash.startsWith(version.toLowerCase(Locale.ROOT));
        } catch (Exception exception) {
            return false;
        }
    }
}
