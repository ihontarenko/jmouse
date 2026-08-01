package org.jmouse.storage.exception;

/**
 * 🚫 A storage key is malformed, or resolves to a location outside the storage root.
 *
 * <p>Raised while <em>constructing</em> a key rather than while using one, so an unsafe key never
 * reaches a backend in the first place.</p>
 */
public class StorageKeyException extends StorageException {

    /**
     * 🏗️ Create a key failure with a message.
     *
     * @param message why the key was refused
     */
    public StorageKeyException(String message) {
        super(message);
    }
}
