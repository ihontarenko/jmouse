package org.jmouse.storage;

import org.jmouse.storage.exception.StorageException;

import java.util.List;
import java.util.Optional;

/**
 * 🗂️ Every backend the application has, with one of them the default.
 *
 * <p>A product does not stay on one backend forever. It starts on local disk, an object store
 * arrives, and from that moment there are files in both — so "the store" has to become a set, or
 * everything written before the move stops opening.</p>
 *
 * <h3>Reads follow the object, writes follow the caller</h3>
 *
 * <p>Every registered object records which backend wrote it, so a read is routed by that and not
 * by what happens to be configured as the default today. That is what lets one listing span every
 * backend at once: the rows are in one table, and each knows where its own bytes are.</p>
 *
 * <p>A write goes to the default unless a caller names another, and whether a caller may name one
 * at all is {@link #isChoiceExposed()}. Left off — which is the shipped default — the arrangement
 * is invisible: uploads land on the default backend and no interface has any reason to mention
 * that there are others.</p>
 */
public interface FileStores {

    /**
     * 🎯 The store everything is written to unless told otherwise.
     *
     * @return the default store
     */
    FileStore defaultStore();

    /**
     * 🏷️ Name of the default store.
     *
     * @return the default backend name
     */
    String defaultBackendName();

    /**
     * 🔎 The store registered under a name.
     *
     * @param backendName name recorded against an object, or configured for a backend
     * @return the store, or empty when no backend answers to that name
     */
    Optional<FileStore> find(String backendName);

    /**
     * 🔎 The store registered under a name, insisting there is one.
     *
     * <p>An object whose backend is no longer configured is unreachable, and saying so is better
     * than falling back to the default — which would read a key out of the wrong bucket and either
     * fail confusingly or, far worse, succeed against somebody else's object.</p>
     *
     * @param backendName name recorded against an object
     * @return the store
     * @throws StorageException when no backend answers to that name
     */
    FileStore require(String backendName);

    /**
     * 📃 Every configured backend, default first.
     *
     * @return the backend names
     */
    List<String> backendNames();

    /**
     * 👁️ Whether callers may choose where an upload goes.
     *
     * <p>What an interface asks before offering the choice at all. When this is false there is
     * nothing to show: {@link #forWriting} sends everything to the default regardless.</p>
     *
     * @return {@code true} when the choice is exposed
     */
    boolean isChoiceExposed();

    /**
     * ✍️ The store an upload should go to.
     *
     * <p>A blank request means "wherever the default is". A named request is honoured only when
     * the choice is exposed — otherwise it is ignored rather than refused, so that a caller
     * carrying a stale preference cannot fail an upload over a setting it has no say in.</p>
     *
     * @param requestedBackendName backend the caller asked for, or {@code null} for the default
     * @return the store to write through
     * @throws StorageException when the choice is exposed and names a backend that does not exist
     */
    FileStore forWriting(String requestedBackendName);
}
