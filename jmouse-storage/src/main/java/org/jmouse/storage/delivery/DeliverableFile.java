package org.jmouse.storage.delivery;

import org.jmouse.core.MediaType;
import org.jmouse.storage.StorageKey;
import org.jmouse.storage.StoredObject;
import org.jmouse.storage.exception.StorageException;

/**
 * 📄 What the delivery layer needs to know about a file, and nothing else.
 *
 * <p>Neutral on purpose: the planner lives in a module that knows nothing about persistence, so it
 * cannot take a registry entity — and a product's own binding row, not the registry, is what knows
 * the name this particular reference should present. Assembling this record is where the two meet.</p>
 *
 * <p>{@link #presentedFilename} is therefore the <em>binding's</em> name rather than the registry's.
 * Once identical bytes back several bindings, the registry's copy is only the name of the first
 * upload, and serving that to everyone would show one user another user's filename.</p>
 *
 * @param storageKey        where the bytes live
 * @param backendName       which backend wrote them, and so which one can serve them
 * @param presentedFilename name this reference shows the user
 * @param contentType       type to serve as (never {@code null})
 * @param sizeBytes         object length in bytes
 * @param sha256            lower-case hex digest, or {@code null} when not yet known
 */
public record DeliverableFile(StorageKey storageKey, String backendName, String presentedFilename,
                              MediaType contentType, long sizeBytes, String sha256) {

    /**
     * 🏗️ Enforce what every delivery decision relies on.
     */
    public DeliverableFile {
        if (storageKey == null) {
            throw new StorageException("A deliverable file must carry a storage key.");
        }

        if (backendName == null || backendName.isBlank()) {
            throw new StorageException("A deliverable file must name the backend holding it.");
        }

        if (presentedFilename == null || presentedFilename.isBlank()) {
            throw new StorageException("A deliverable file must carry a filename to present.");
        }
    }

    /**
     * 🏗️ A file just written, delivered under the name it arrived as.
     *
     * @param object      the write receipt
     * @param backendName backend that produced the receipt
     * @return the deliverable file
     */
    public static DeliverableFile of(StoredObject object, String backendName) {
        return new DeliverableFile(object.key(), backendName, object.key().value(),
                                   object.contentType(), object.sizeBytes(), object.sha256());
    }

    /**
     * 🔐 Whether this file can answer a conditional request.
     *
     * <p>A file stored before the registry existed has no digest until it is backfilled, and an
     * entity tag invented for it would be a lie a client would cache.</p>
     *
     * @return {@code true} when a digest is known
     */
    public boolean hasDigest() {
        return sha256 != null && !sha256.isBlank();
    }

    /**
     * 🏷️ This file under a different presented name.
     *
     * @param presentedFilename name to show the user
     * @return a copy carrying that name
     */
    public DeliverableFile presentedAs(String presentedFilename) {
        return new DeliverableFile(storageKey, backendName, presentedFilename, contentType, sizeBytes,
                                   sha256);
    }
}
