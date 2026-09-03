package org.jmouse.files.directory;

import org.jmouse.files.exception.DirectoryException;
import org.jmouse.storage.policy.AcceptanceMode;
import org.jmouse.storage.policy.UploadPolicy;

import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 🛃 What a folder accepts — the {@code upload} kind of directory configuration.
 *
 * <p>A folder carrying one of these <strong>replaces</strong> the installation's acceptance policy for
 * everything filed into it and into everything beneath it that carries none of its own. It does not add
 * to it, and it does not merge with an ancestor's: the nearest configuration wins entirely, because a
 * rule assembled out of four ancestors is a rule nobody can read off one screen.</p>
 *
 * <h3>⚠️ The mode belongs to the folder too</h3>
 *
 * <p>Not only the lists. A folder <em>stricter</em> than a denylist installation is exactly an
 * {@link AcceptanceMode#ALLOW_LIST} on one row, and without the mode here the mechanism would only work
 * in the loosening direction — which is half a feature and the more alarming half.</p>
 *
 * <h3>⚠️ There is no floor, and that is a decision rather than an oversight</h3>
 *
 * <p>The library reserves no type. A folder may admit {@code exe}, {@code jar}, {@code php}, {@code
 * html}. What carries the risk instead is <em>access</em>: a branch admitting active content is closed
 * with grants, which the directory scope hierarchy already does. Two safeguards are untouched by that
 * and worth saying out loud, because "no floor" reads more alarming than it is — active content types
 * are served as an attachment on every delivery path whoever let the bytes in, and who may read a
 * directory is a separate axis entirely.</p>
 *
 * @param mode         how the lists are read
 * @param contentTypes bare {@code type/subtype} values, without parameters, lower-cased
 * @param extensions   extensions without their dot, lower-cased
 * @param maxSizeBytes largest content accepted, or {@code null} to keep the installation's limit —
 *                     a folder may widen the lists without having an opinion about size
 */
public record DirectoryUploadConfiguration(AcceptanceMode mode, Set<String> contentTypes,
                                           Set<String> extensions, Long maxSizeBytes) {

    /** The kind this record is the payload of. */
    public static final DirectoryConfigurationKind<DirectoryUploadConfiguration> KIND =
            DirectoryConfigurationKind.of("upload", DirectoryUploadConfiguration.class);

    /**
     * 🏗️ Normalise on the way in, so what is stored is what will actually be matched.
     *
     * <p>⚠️ {@link UploadPolicy} lower-cases what it is given and compares bare types, so an
     * un-normalised rule — {@code .HTML}, {@code text/html; charset=utf-8} — looks perfectly correct in
     * the database and matches nothing at all. Normalising here rather than at the route means every
     * way in gets it, including a product writing one directly.</p>
     */
    public DirectoryUploadConfiguration {
        if (mode == null) {
            throw new DirectoryException(
                "An upload configuration has to say whether its lists admit or refuse.");
        }

        contentTypes = normalised(contentTypes, DirectoryUploadConfiguration::baseType);
        extensions   = normalised(extensions, DirectoryUploadConfiguration::withoutDot);

        if (maxSizeBytes != null && maxSizeBytes <= 0) {
            throw new DirectoryException(
                "A folder's size limit has to be a positive number of bytes, or absent to keep the "
                + "installation's.");
        }
    }

    /**
     * 🕳️ Whether this rule admits nothing at all — an allowlist with both lists empty.
     *
     * <p>A folder nobody can upload to. It may genuinely be what somebody wants, so this reports the
     * fact rather than refusing it; the route decides.</p>
     *
     * @return {@code true} when nothing could ever be accepted here
     */
    public boolean admitsNothing() {
        return mode == AcceptanceMode.ALLOW_LIST && contentTypes.isEmpty() && extensions.isEmpty();
    }

    /**
     * 🛃 This configuration as the policy that enforces it.
     *
     * @param installationMaxSizeBytes the limit to keep where this folder states none
     * @return the policy
     */
    public UploadPolicy asPolicy(long installationMaxSizeBytes) {
        return new UploadPolicy(mode, contentTypes, extensions,
                                maxSizeBytes == null ? installationMaxSizeBytes : maxSizeBytes);
    }

    private static Set<String> normalised(Set<String> values,
                                          java.util.function.UnaryOperator<String> shape) {
        if (values == null) {
            return Set.of();
        }

        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(value -> shape.apply(value.trim().toLowerCase(Locale.ROOT)))
                .filter(value -> !value.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * 🏷️ {@code text/html; charset=utf-8} is the same type as {@code text/html}.
     */
    private static String baseType(String contentType) {
        int parameters = contentType.indexOf(';');

        return parameters < 0 ? contentType : contentType.substring(0, parameters).trim();
    }

    /**
     * 📄 {@code .zip} and {@code zip} are the same extension.
     */
    private static String withoutDot(String extension) {
        return extension.startsWith(".") ? extension.substring(1) : extension;
    }
}
