package org.jmouse.storage.key;

import org.jmouse.storage.FileStore;
import org.jmouse.storage.StorageKey;

/**
 * 🗺️ Decides where content is laid out.
 *
 * <p>Separating layout from the {@link FileStore} that physically writes is what lets the layout
 * policy — per owner, per content class, content-addressed, flat — change without touching a
 * caller or a backend. It is also the single mechanism: an application must not have one layout
 * for uploads and a different one for documents, which is the situation this replaces.</p>
 */
@FunctionalInterface
public interface StorageKeyStrategy {

    /**
     * 🔑 Compose the key content should be written under.
     *
     * @param request what the caller knows about the content
     * @return the key to write at
     */
    StorageKey compose(StorageKeyRequest request);
}
