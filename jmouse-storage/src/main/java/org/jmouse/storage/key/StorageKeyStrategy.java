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
public interface StorageKeyStrategy {

    /**
     * 🔑 Compose the key content should be written under.
     *
     * @param request what the caller knows about the content
     * @return the key to write at
     */
    StorageKey compose(StorageKeyRequest request);

    /**
     * 🔐 Whether this layout needs the content's digest before it can place it.
     *
     * <p>Content-addressed layouts do, and that reverses the usual order: the bytes have to be
     * read once before anything knows where they go. A caller that sees {@code true} spools and
     * digests first, which is also what makes deduplication possible — by then the digest is in
     * hand and the registry can be asked whether these exact bytes are already stored.</p>
     *
     * <p>Layouts that place content by owner, category or anything else known up front return
     * {@code false} and cost no extra pass.</p>
     *
     * @return {@code true} when {@link StorageKeyRequest#contentDigest()} must be set
     */
    default boolean requiresContentDigest() {
        return false;
    }
}
