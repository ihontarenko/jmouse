package org.jmouse.storage.policy;

import org.jmouse.storage.Content;
import org.jmouse.storage.ContentTypes;
import org.jmouse.storage.configuration.StorageSettings;
import org.jmouse.storage.configuration.UploadSettings;
import org.jmouse.storage.exception.UploadRejectedException;

import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 🛃 What may enter storage, and how large it may be.
 *
 * <p>One component in two modes rather than two components: a product that blocks executables and
 * markup, and a product that admits only images, PDF and Office formats, are configuring the same
 * rule in opposite directions.</p>
 *
 * <p>Declared content type and extension are checked <strong>independently</strong>, because a
 * client controls both and may lie about either — an executable renamed {@code invoice.pdf} and an
 * executable declared as {@code application/pdf} are two different lies and each needs its own
 * check.</p>
 *
 * <p>A policy decides; it never writes. Rejected content therefore leaves nothing behind, because
 * nothing was ever opened.</p>
 */
public final class UploadPolicy {

    private static final long BYTES_PER_MEGABYTE = 1024L * 1024L;

    private final AcceptanceMode mode;
    private final Set<String>    contentTypes;
    private final Set<String>    extensions;
    private final long           maxSizeBytes;

    /**
     * 🏗️ Build a policy from its parts.
     *
     * @param mode         how the lists are read
     * @param contentTypes bare {@code type/subtype} values
     * @param extensions   extensions without their dot
     * @param maxSizeBytes largest content accepted
     */
    public UploadPolicy(AcceptanceMode mode, Set<String> contentTypes, Set<String> extensions,
                        long maxSizeBytes) {
        this.mode         = mode;
        this.contentTypes = lowerCased(contentTypes);
        this.extensions   = lowerCased(extensions);
        this.maxSizeBytes = maxSizeBytes;
    }

    /**
     * 🏗️ Build a policy entirely out of configuration — no subclass, no code change, no rebuild.
     *
     * @param settings the active storage settings
     * @return the configured policy
     */
    public static UploadPolicy of(StorageSettings settings) {
        UploadSettings upload = settings.upload();
        return new UploadPolicy(upload.mode(), upload.contentTypes(), upload.extensions(),
                                settings.maxSizeBytes());
    }

    /**
     * ✅ Decide whether content may be stored.
     *
     * @param content the content offered
     * @throws UploadRejectedException describing the first rule the content breaks
     */
    public void accept(Content content) {
        if (content.hasDeclaredSize()) {
            ensureNotEmpty(content.declaredSize());
            ensureWithinSizeLimit(content.declaredSize());
        }

        ensureContentTypeAccepted(ContentTypes.baseType(content.declaredContentType()));
        ensureExtensionAccepted(content.extension());
    }

    /**
     * 📏 Reject content larger than the configured maximum.
     *
     * <p>A negative size means "unknown" — a remote server that omits {@code Content-Length}, a
     * generated stream — and passes. The size a client claims is not evidence of anything, so this
     * is also the method a caller re-runs against
     * {@link org.jmouse.storage.StoredObject#sizeBytes()} once the bytes have actually arrived,
     * deleting the object if the real size breaks the limit a lying declaration slipped past.</p>
     *
     * @param sizeBytes size to check
     * @throws UploadRejectedException when the size exceeds the maximum
     */
    public void ensureWithinSizeLimit(long sizeBytes) {
        if (sizeBytes > maxSizeBytes) {
            throw new UploadRejectedException(
                    "File size exceeds the maximum of %d MB.".formatted(maxSizeBytes / BYTES_PER_MEGABYTE));
        }
    }

    /**
     * 🕳️ Reject content with no bytes in it.
     *
     * <p>An empty upload is always a mistake — a failed browser selection, a truncated import — and
     * storing it costs an object that nothing will ever want to read.</p>
     *
     * @param sizeBytes size to check
     * @throws UploadRejectedException when the content is empty
     */
    public void ensureNotEmpty(long sizeBytes) {
        if (sizeBytes == 0) {
            throw new UploadRejectedException("Cannot upload an empty file.");
        }
    }

    /**
     * 🏷️ Check the declared content type.
     *
     * <p>Content that declares no type at all is not judged on its type — there is nothing to
     * judge. The extension check still applies to it.</p>
     *
     * @param baseType bare {@code type/subtype}, or {@code null} when nothing was declared
     */
    private void ensureContentTypeAccepted(String baseType) {
        if (baseType == null) {
            return;
        }

        if (isRefused(contentTypes, baseType)) {
            throw new UploadRejectedException("File type '%s' is not allowed.".formatted(baseType));
        }
    }

    /**
     * 📄 Check the extension.
     *
     * <p>Under a denylist, content with no extension is not on the list and so passes. Under an
     * allowlist it is likewise not on the list, and so is refused — which is the point of an
     * allowlist.</p>
     *
     * @param extension lower-cased extension without its dot, possibly empty
     */
    private void ensureExtensionAccepted(String extension) {
        if (extension.isEmpty() && mode == AcceptanceMode.DENYLIST) {
            return;
        }

        if (isRefused(extensions, extension)) {
            throw new UploadRejectedException(extension.isEmpty()
                                                      ? "A file extension is required."
                                                      : "File extension '.%s' is not allowed.".formatted(extension));
        }
    }

    /**
     * 🚦 Read a list according to the active mode.
     *
     * @param listed    the configured list
     * @param candidate the value under judgement
     * @return {@code true} when the candidate must be refused
     */
    private boolean isRefused(Set<String> listed, String candidate) {
        if (mode == AcceptanceMode.ALLOWLIST) {
            return !listed.contains(candidate);
        }

        return listed.contains(candidate);
    }

    private static Set<String> lowerCased(Set<String> values) {
        if (values == null) {
            return Set.of();
        }

        return values.stream().map(value -> value.toLowerCase(Locale.ROOT)).collect(Collectors.toUnmodifiableSet());
    }
}
