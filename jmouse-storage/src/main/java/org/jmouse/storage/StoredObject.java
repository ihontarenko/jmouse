package org.jmouse.storage;

import org.jmouse.core.MediaType;

/**
 * ✅ The receipt for a completed write.
 *
 * <p>Unlike {@link ObjectDescription} this carries the digest, because the digest is computed in
 * the same pass as the bytes — establishing it afterwards would mean reading the object a second
 * time. It doubles as a content identity for deduplication and as a strong entity tag for
 * conditional requests, so neither costs an extra column or an extra read.</p>
 *
 * @param key         where the object was written
 * @param sizeBytes   number of bytes that actually arrived, not the number that was claimed
 * @param contentType type the object is served as (never {@code null})
 * @param sha256      lower-case hex SHA-256 of the bytes
 */
public record StoredObject(StorageKey key, long sizeBytes, MediaType contentType, String sha256) {

    /**
     * 📋 This object without its digest, for callers that only need size and type.
     *
     * @return the matching description
     */
    public ObjectDescription describe() {
        return new ObjectDescription(key, sizeBytes, contentType);
    }
}
