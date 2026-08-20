package org.jmouse.avatar;

import org.jmouse.storage.exception.StorageException;

/**
 * 🔍 No avatar at that address.
 *
 * <p>⚠️ Extends the storage exception so that the starter's problem-detail advice already answers it as
 * a 404 — a product adopting this module gets the right status without writing a handler, and one that
 * has its own handler keeps it.</p>
 */
public class AvatarNotFoundException extends StorageException {

    /**
     * 🏗️ Report a missing avatar.
     *
     * @param storedFileId the address that resolved to nothing
     */
    public AvatarNotFoundException(String storedFileId) {
        super("No such avatar: %s".formatted(storedFileId));
    }
}
