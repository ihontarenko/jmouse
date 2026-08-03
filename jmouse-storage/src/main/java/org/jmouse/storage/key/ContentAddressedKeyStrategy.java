package org.jmouse.storage.key;

import org.jmouse.storage.StorageKey;
import org.jmouse.storage.exception.StorageKeyException;

/**
 * 🧬 Lays content out by what it <em>is</em>:
 * {@code {namespace}/sha256/{aa}/{bb}/{digest}}.
 *
 * <p>The layout deduplication needs. Two callers uploading the same bytes compose the same key, so
 * the second write lands on the object the first one made and one copy backs both bindings.</p>
 *
 * <h3>Why the namespace stays and the owner goes</h3>
 *
 * <p>Owner-namespacing and deduplication cannot both be true. The moment bytes are shared, the
 * object physically lives under whoever uploaded it first — so a per-owner prefix stops meaning
 * anything exactly when deduplication starts working, and one user's file sits inside another's
 * directory. The owner therefore leaves the key entirely; who holds a file is a fact about a
 * binding, and bindings live in a table.</p>
 *
 * <p>A non-owner namespace is kept, which is what makes this a hybrid rather than a flat
 * content-addressed store. It costs storing identical bytes once per namespace, and buys a bucket
 * a human can still navigate, per-prefix lifecycle rules, and per-prefix quotas. Deduplication
 * within a content class is where nearly all the duplication actually is.</p>
 *
 * <h3>Two consequences worth knowing before adopting it</h3>
 *
 * <p><strong>The key carries no extension.</strong> Content type therefore has to come from the
 * registry rather than from the object's name — which is more accurate anyway, since an extension
 * is whatever the uploader typed. A backend that inferred type from the key must stop.</p>
 *
 * <p><strong>The digest has to be known first.</strong> That reverses the usual order — bytes are
 * read once before anything knows where they go — which is why
 * {@link #requiresContentDigest()} is {@code true} and callers spool before composing.</p>
 *
 * <p>Existing keys are unaffected. A key is stored verbatim against whatever row already has it,
 * so switching to this layout changes where <em>new</em> objects land and nothing about the old
 * ones.</p>
 */
public class ContentAddressedKeyStrategy implements StorageKeyStrategy {

    /**
     * 🔐 Marks the digest algorithm in the key, so a future move to another one is a new prefix
     * rather than an ambiguous directory of mixed hashes.
     */
    public static final String DIGEST_SEGMENT = "sha256";

    /**
     * 📂 Characters of the digest per fan-out directory. Two levels of two hex characters give
     * 65 536 leaves, which keeps any single directory small on a file system and shards evenly
     * across an object store's key space.
     */
    private static final int FAN_OUT_LENGTH = 2;

    private static final int  FAN_OUT_LEVELS   = 2;
    private static final int  MINIMUM_DIGEST   = FAN_OUT_LENGTH * FAN_OUT_LEVELS + 1;
    private static final char SEPARATOR        = '/';

    @Override
    public boolean requiresContentDigest() {
        return true;
    }

    @Override
    public StorageKey compose(StorageKeyRequest request) {
        String digest = requireDigest(request);

        StringBuilder key = new StringBuilder();

        appendSegment(key, request.namespace());
        appendSegment(key, DIGEST_SEGMENT);

        for (int level = 0; level < FAN_OUT_LEVELS; level++) {
            appendSegment(key, digest.substring(level * FAN_OUT_LENGTH, (level + 1) * FAN_OUT_LENGTH));
        }

        appendSegment(key, digest);

        return StorageKey.of(key.toString());
    }

    /**
     * 🔐 The digest, insisting the caller established it.
     *
     * <p>Failing loudly here rather than inventing a placeholder is the whole point: a
     * content-addressed key derived from anything but the content is a key that deduplicates
     * nothing and collides with itself.</p>
     *
     * @param request the request being composed
     * @return the digest
     * @throws StorageKeyException when no usable digest was supplied
     */
    private String requireDigest(StorageKeyRequest request) {
        String digest = request.contentDigest();

        if (digest == null || digest.length() < MINIMUM_DIGEST) {
            throw new StorageKeyException(
                    "A content-addressed key needs the content's digest — digest the bytes before "
                            + "composing the key, or use a layout that places content by owner.");
        }

        return digest;
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
}
