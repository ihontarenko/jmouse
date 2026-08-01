package org.jmouse.storage;

import org.jmouse.core.MediaType;

/**
 * 📋 What a backend knows about a stored object without transferring its bytes.
 *
 * @param key         where the object lives
 * @param sizeBytes   object length in bytes
 * @param contentType type the object is served as (never {@code null})
 */
public record ObjectDescription(StorageKey key, long sizeBytes, MediaType contentType) {
}
