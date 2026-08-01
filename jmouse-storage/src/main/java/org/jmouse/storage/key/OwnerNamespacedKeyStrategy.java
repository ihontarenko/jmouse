package org.jmouse.storage.key;

import org.jmouse.core.IdGenerator;
import org.jmouse.core.PrefixedIdGenerator;
import org.jmouse.storage.StorageKey;

/**
 * 🏠 Lays content out per owner: {@code {namespace}/u/{ownerIdentifier}/{segments…}/{base}.{ext}}.
 *
 * <p>The layout is deliberately unchanged from the one already in production, so adopting this
 * library is a code change and not a data movement — the same inputs produce byte-identical keys,
 * and every key already persisted keeps resolving.</p>
 *
 * <p><strong>One caller does change shape.</strong> A product whose upload path bypassed its own
 * layout policy and wrote {@code {ownerIdentifier}/{generated}.{extension}} directly will now emit
 * {@code u/{ownerIdentifier}/{generated}.{extension}}. That is the intended collapse of two
 * competing layouts into one, not a regression — but it applies to <em>new</em> writes only, and a
 * cutover has to keep resolving the old keys it has already persisted. Content with no owner needs
 * an explicit stand-in identifier such as {@code public}, since a layout cannot namespace by an
 * owner that is absent.</p>
 *
 * <p>A blank namespace is omitted rather than rendered, giving {@code u/{ownerIdentifier}/…}.
 * A blank filename base is generated, which is what an upload wants: the client's own filename is
 * kept on the record, never in the key, where it would be attacker-controlled.</p>
 *
 * <p>Swapping the layout — content-addressed, hybrid, anything else — is a different implementation
 * of {@link StorageKeyStrategy} and touches nothing else.</p>
 */
public class OwnerNamespacedKeyStrategy implements StorageKeyStrategy {

    /**
     * 👤 Marks the owner's slice of the tree, so a per-owner prefix is greppable and a per-owner
     * lifecycle rule is expressible.
     */
    public static final String OWNER_SEGMENT = "u";

    private static final char SEPARATOR     = '/';
    private static final char EXTENSION_DOT = '.';

    private final IdGenerator<String, String> idGenerator;

    /**
     * 🏗️ Use the default prefixed identifier generator for generated filenames.
     */
    public OwnerNamespacedKeyStrategy() {
        this(PrefixedIdGenerator.defaultGenerator());
    }

    /**
     * 🏗️ Use a specific generator for filenames the caller does not supply.
     *
     * @param idGenerator produces filename bases
     */
    public OwnerNamespacedKeyStrategy(IdGenerator<String, String> idGenerator) {
        this.idGenerator = idGenerator;
    }

    @Override
    public StorageKey compose(StorageKeyRequest request) {
        StringBuilder key = new StringBuilder();

        appendSegment(key, request.namespace());
        appendSegment(key, OWNER_SEGMENT);
        appendSegment(key, request.ownerIdentifier());

        for (String segment : request.segments()) {
            appendSegment(key, segment);
        }

        appendSegment(key, resolveFilenameBase(request));

        if (request.extension() != null && !request.extension().isBlank()) {
            key.append(EXTENSION_DOT).append(request.extension());
        }

        return StorageKey.of(key.toString());
    }

    /**
     * ➕ Append a path segment, skipping blanks and keeping separators single.
     *
     * @param key     key being assembled
     * @param segment segment to append
     */
    private void appendSegment(StringBuilder key, String segment) {
        if (segment == null || segment.isBlank()) {
            return;
        }

        if (!key.isEmpty()) {
            key.append(SEPARATOR);
        }

        key.append(segment);
    }

    /**
     * 🏷️ The caller's filename base, or a generated one when the caller has no opinion.
     *
     * @param request the request being composed
     * @return a non-blank filename base
     */
    private String resolveFilenameBase(StorageKeyRequest request) {
        String filenameBase = request.filenameBase();
        boolean supplied = filenameBase != null && !filenameBase.isBlank();
        return supplied ? filenameBase : idGenerator.generate();
    }
}
