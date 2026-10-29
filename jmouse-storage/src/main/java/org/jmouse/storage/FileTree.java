package org.jmouse.storage;

import java.time.Instant;
import java.util.List;

/**
 * 🌳 A store whose objects stand in folders a reader can walk: a directory on the server's disk mounted as it is, a
 * bucket's prefixes. A capability beside {@link FileStore}, not part of it — a content-addressed store has keys, not
 * folders, and says so by not being one.
 *
 * <p>Every key stays inside the store: a {@link StorageKey} cannot climb out, and a backend re-checks what it resolves.
 * A folder is removed only empty — taking a tree away whole is the caller's decision, file by file.</p>
 */
public interface FileTree {

    /**
     * 📄 One thing in a folder.
     *
     * @param key        where it is, from the store's top
     * @param name       its own name, the key's last segment
     * @param folder     whether it holds other entries
     * @param sizeBytes  its length; 0 for a folder
     * @param modifiedAt when it last changed
     */
    record Entry(StorageKey key, String name, boolean folder, long sizeBytes, Instant modifiedAt) {
    }

    /** 📂 The store's top folder: its folders first, then its files, each by name. */
    List<Entry> top();

    /**
     * 📂 One folder's entries: its folders first, then its files, each by name.
     *
     * @throws org.jmouse.storage.exception.ObjectNotFoundException when no folder stands there
     */
    List<Entry> list(StorageKey folder);

    /** ➕ A folder made, with the folders above it; one that is there already is left as it is. */
    void createFolder(StorageKey folder);

    /**
     * 🚚 A file or a folder moved — renamed, or filed elsewhere.
     *
     * @throws org.jmouse.storage.exception.StorageException when something already stands at {@code to}
     */
    void move(StorageKey from, StorageKey to);

    /**
     * 🗑️ An empty folder removed.
     *
     * @throws org.jmouse.storage.exception.StorageException when the folder still holds something
     */
    void deleteFolder(StorageKey folder);
}
