package org.jmouse.storage.administration;

import org.jmouse.storage.jpa.StoredFile;

import java.time.LocalDateTime;

/**
 * 📦 One stored object, as an administrator sees it.
 *
 * <p>⚠️ {@code digested} rather than the digest itself. The hash is not a secret, but printing 64 hex
 * characters per row in a table nobody reads them from is noise — what an administrator actually needs
 * to know is whether this object can answer a conditional request and take part in deduplication, and
 * that is a yes or a no.</p>
 *
 * @param id           the registry identifier
 * @param storageKey   where the bytes live
 * @param originalName the name the first upload of these bytes arrived under
 * @param contentType  what they are
 * @param sizeBytes    how large
 * @param backend      which backend holds them
 * @param digested     whether a digest is known
 * @param createdAt    when it was registered
 */
public record RegistryEntry(String id, String storageKey, String originalName, String contentType,
                            long sizeBytes, String backend, boolean digested, LocalDateTime createdAt) {

    /**
     * 🏗️ Describe a stored object.
     *
     * @param storedFile the registry row
     * @return the view
     */
    public static RegistryEntry of(StoredFile storedFile) {
        return new RegistryEntry(
            storedFile.getIdentifier(), storedFile.getStorageKey().value(),
            storedFile.getOriginalName(), storedFile.getContentType().toString(),
            storedFile.getSizeBytes(), storedFile.getBackend(),
            storedFile.getSha256() != null && !storedFile.getSha256().isBlank(),
            storedFile.getCreatedAt());
    }
}
