package org.jmouse.storage.jpa;

import org.jmouse.storage.StorageKey;
import org.jmouse.storage.StoredObject;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 📇 The one place a product learns what has been stored.
 *
 * <p>Products bind to rows in this registry; they do not query the table themselves. Keeping every
 * access behind this interface is what lets the registry change how it is indexed, paged or
 * digested without a product noticing — and it is why no repository over
 * {@link StoredFile} ships anywhere.</p>
 *
 * <p><strong>Transaction demarcation belongs to the caller.</strong> Nothing here opens, commits or
 * rolls back anything: a registration must land or not land together with the product row that
 * points at it, and only the caller knows what "together" means. A library that started its own
 * transaction would make that impossible.</p>
 */
public interface StoredFileRegistry {

    /**
     * ✅ Record an object that has just been written.
     *
     * <p>The digest stored is the one {@link StoredObject#sha256()} carries — computed during the
     * write, in the same pass as the bytes — never a second read over the object.</p>
     *
     * @param object       receipt returned by the write
     * @param originalName name the content arrived under
     * @return the registered row
     */
    StoredFile register(StoredObject object, String originalName);

    /**
     * 🔎 Look a row up by its identifier.
     *
     * @param identifier registry identifier
     * @return the row, or empty when nothing is registered under it
     */
    Optional<StoredFile> find(String identifier);

    /**
     * 🔎 Look a row up by the key its bytes live at.
     *
     * <p>What a cutover uses to avoid registering the same object twice, since an existing product
     * row already carries the key verbatim.</p>
     *
     * @param key storage key
     * @return the row, or empty when the key is not registered
     */
    Optional<StoredFile> findByStorageKey(StorageKey key);

    /**
     * 🔎 Look a row up by content identity.
     *
     * <p>What deduplication asks before writing: bytes already registered under this digest need
     * not be written again, and the new binding points at the row that exists.</p>
     *
     * @param sha256 lower-case hex digest
     * @return a row holding those bytes, or empty when they are new
     */
    Optional<StoredFile> findBySha256(String sha256);

    /**
     * 📃 A page of rows in registration order, oldest first.
     *
     * @param offset how many rows to skip
     * @param limit  how many rows to return
     * @return the page, possibly empty
     */
    List<StoredFile> list(int offset, int limit);

    /**
     * 🧹 A page of rows registered before a cut-off, walked by identifier rather than by offset.
     *
     * <p>This is what the sweeper scans. The cut-off is the grace period, and it is what stops an
     * object written moments ago by a transaction that has not committed yet from looking like an
     * orphan.</p>
     *
     * <p>Paging is keyset rather than offset-based on purpose: a sweep deletes rows as it walks,
     * and every deletion would shift an offset underneath it — skipping exactly the rows that had
     * just been proven to be orphans. Resuming after the last identifier seen is stable under
     * concurrent deletion, which also makes an interrupted sweep resumable rather than confusing.</p>
     *
     * <p>Whether a row is actually referenced is not asked here. The set of live references is the
     * union of what every product reported, which can be large enough that turning it into a
     * {@code NOT IN} clause would be worse than checking it in memory — so
     * {@link org.jmouse.storage.jpa.sweeper.OrphanSweeper} keeps that judgement.</p>
     *
     * @param writtenBefore   only rows registered strictly before this are returned
     * @param afterIdentifier resume after this identifier, or {@code null} to start from the first
     * @param limit           how many rows to return
     * @return the page in identifier order, possibly empty
     */
    List<StoredFile> listRegisteredBefore(LocalDateTime writtenBefore, String afterIdentifier, int limit);

    /**
     * 🗑️ Remove a row.
     *
     * <p>Removes the record, never the bytes: reclaiming those is the sweeper's job, precisely so
     * that deleting one binding cannot destroy an object another binding still holds.</p>
     *
     * @param storedFile the row to remove
     */
    void remove(StoredFile storedFile);

    /**
     * 🔢 How many objects are registered.
     *
     * @return the row count
     */
    long count();
}
