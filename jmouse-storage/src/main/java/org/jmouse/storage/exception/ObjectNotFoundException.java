package org.jmouse.storage.exception;

import org.jmouse.storage.StorageKey;

/**
 * 🔍 Nothing is stored under the requested key.
 *
 * <p>Note that {@code delete} deliberately does <strong>not</strong> raise this — removing an
 * object that is already gone is a success, not a failure.</p>
 */
public class ObjectNotFoundException extends StorageException {

    /**
     * 🏗️ Report a missing object.
     *
     * @param key the key that resolved to nothing
     */
    public ObjectNotFoundException(StorageKey key) {
        super("No stored object under key '%s'".formatted(key.value()));
    }
}
