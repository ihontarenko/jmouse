package org.jmouse.files.jpa;

import jakarta.persistence.EntityManager;
import org.jmouse.files.OwnerReference;
import org.jmouse.files.exception.ManagedFileNotFoundException;
import org.jmouse.storage.jpa.StoredFile;

import java.util.List;
import java.util.Optional;

/**
 * 📄 The file rows: recording one, finding one, renaming one, removing one.
 *
 * <h3>What it deliberately does not do</h3>
 *
 * <p><strong>It does not take bytes into storage.</strong> That is
 * {@code StoredFileIngestion}'s, and keeping the two apart is what lets a product ingest once and
 * record several times — a document uploaded twice by two people costs one stored object and two of
 * these rows, each with its own name and uploader.</p>
 *
 * <p><strong>It does not authorize anything.</strong> Every product in this workspace gates at the
 * route through one engine, and a second opinion here would be a rule somebody edits in one place.</p>
 *
 * <p>Transaction demarcation is the caller's throughout.</p>
 */
public class ManagedFiles {

    private final EntityManager entityManager;
    private final FileBindings  bindings;

    /**
     * 🏗️ Work over the application's persistence context.
     *
     * @param entityManager the persistence context
     * @param bindings      filing, so that removing a file can take its places with it
     */
    public ManagedFiles(EntityManager entityManager, FileBindings bindings) {
        this.entityManager = entityManager;
        this.bindings      = bindings;
    }

    /**
     * 📝 Record a file that has already been taken into storage.
     *
     * @param identifier  identifier the product minted
     * @param displayName what to call it here
     * @param storedFile  the bytes, as the registry recorded them
     * @param uploadedBy  who put it there, or {@code null} where nobody is signed in
     * @return the recorded file
     */
    public ManagedFile record(String identifier, String displayName, StoredFile storedFile,
                              String uploadedBy) {
        ManagedFile file = new ManagedFile(identifier, displayName, storedFile, uploadedBy);

        entityManager.persist(file);

        return file;
    }

    /**
     * 🔎 One file, if it is there.
     *
     * @param identifier the file
     * @return the file, or empty
     */
    public Optional<ManagedFile> find(String identifier) {
        return Optional.ofNullable(entityManager.find(ManagedFile.class, identifier));
    }

    /**
     * 🔎 One file, or a refusal naming it.
     *
     * @param identifier the file
     * @return the file
     */
    public ManagedFile require(String identifier) {
        return find(identifier).orElseThrow(() -> new ManagedFileNotFoundException(identifier));
    }

    /**
     * 🏷️ Rename one.
     *
     * <p>⚠️ Renames <strong>this</strong> row, never the stored object: the object may back other
     * files whose owners did not ask for anything to change.</p>
     *
     * @param identifier  the file
     * @param displayName the new name
     * @return the renamed file
     */
    public ManagedFile rename(String identifier, String displayName) {
        ManagedFile file = require(identifier);

        file.renameTo(displayName);

        return file;
    }

    /**
     * 📂 Everything filed against one owner.
     *
     * <p>One query rather than a binding lookup followed by a file lookup each, because a directory
     * listing renders the file and the order together.</p>
     *
     * @param owner what holds them
     * @return the files, in the order they are filed
     */
    public List<ManagedFile> listFiledUnder(OwnerReference owner) {
        return entityManager.createQuery(
                "SELECT file FROM ManagedFile file, FileBinding binding "
                + "WHERE binding.fileId = file.id "
                + "AND binding.ownerType = :ownerType AND binding.ownerId = :ownerId "
                + "ORDER BY binding.sortOrder, file.displayName", ManagedFile.class)
            .setParameter("ownerType", owner.ownerType())
            .setParameter("ownerId", owner.ownerId())
            .getResultList();
    }

    /**
     * 🗑️ Remove a file and everywhere it was filed.
     *
     * <p>⚠️ <strong>The bytes stay, and that is not an oversight.</strong> Keys are content-addressed,
     * so two people who uploaded the same document share one stored object; deleting it because one of
     * them changed their mind takes the other's file away too. Reclaiming what nothing points at is
     * the orphan sweeper's job, and it finds this table on its own.</p>
     *
     * @param identifier the file
     */
    public void delete(String identifier) {
        ManagedFile file = require(identifier);

        bindings.unbindAll(identifier);
        entityManager.remove(file);
    }
}
