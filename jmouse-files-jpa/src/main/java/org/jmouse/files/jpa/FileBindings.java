package org.jmouse.files.jpa;

import jakarta.persistence.EntityManager;
import org.jmouse.files.OwnerReference;

import java.util.List;
import java.util.Optional;

/**
 * 🔗 Filing, unfiling and re-filing — the whole of what a place means here.
 *
 * <p>Transaction demarcation is the caller's, as everywhere else in these libraries. Nothing here
 * opens one, and every method is written to be correct inside whatever transaction the product's own
 * service already started.</p>
 */
public class FileBindings {

    private final EntityManager entityManager;

    /**
     * 🏗️ Work over the application's persistence context.
     *
     * @param entityManager the persistence context
     */
    public FileBindings(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /**
     * 🔗 File a file against something, adding to whatever it is already filed against.
     *
     * <p>Filing the same pair twice is a no-op rather than a failure: a client retrying an upload it
     * is not sure completed should not get an error for the one outcome it wanted.</p>
     *
     * @param fileId the file
     * @param owner  what should hold it
     * @return the binding, whether it was made now or already existed
     */
    public FileBinding bind(String fileId, OwnerReference owner) {
        FileBinding existing = entityManager.find(
            FileBinding.class, new FileBindingId(fileId, owner.ownerType(), owner.ownerId()));

        if (existing != null) {
            return existing;
        }

        FileBinding binding = new FileBinding(fileId, owner, nextSortOrder(owner));

        entityManager.persist(binding);

        return binding;
    }

    /**
     * ✂️ Stop filing a file against something.
     *
     * <p>⚠️ Removes the binding and nothing else. The file row stays — it may be filed elsewhere — and
     * the bytes certainly stay, because another file row may share the same stored object.</p>
     *
     * @param fileId the file
     * @param owner  what should stop holding it
     * @return {@code true} when there was a binding to remove
     */
    public boolean unbind(String fileId, OwnerReference owner) {
        FileBinding binding = entityManager.find(
            FileBinding.class, new FileBindingId(fileId, owner.ownerType(), owner.ownerId()));

        if (binding == null) {
            return false;
        }

        entityManager.remove(binding);

        return true;
    }

    /**
     * 📦 Move a file to another owner <em>of the same kind</em>, leaving other kinds alone.
     *
     * <p>⚠️ <strong>This is how "a file is in exactly one directory" is expressed.</strong> The schema
     * allows many bindings because attachments genuinely have many; a tree wants one, and gets it by
     * replacing rather than by a constraint that would make the other products unrepresentable.</p>
     *
     * <p>⚠️ And it replaces only the matching <em>kind</em>: re-filing a document into a different
     * directory must not quietly detach it from the issue it is also attached to.</p>
     *
     * @param fileId the file
     * @param owner  the owner it should now be filed against
     * @return the new binding
     */
    public FileBinding refile(String fileId, OwnerReference owner) {
        // ⚠️ Removed THROUGH the persistence context, not with a bulk DELETE. A bulk statement bypasses
        // the context, so the row it removed stays in there as a managed instance — and the very next
        // line asks find() for exactly that key, gets the stale instance back, and returns it as though
        // the binding had been made. Re-filing would silently do nothing. The set is one row in every
        // real case, so there was never anything to optimise here.
        for (FileBinding existing : matching(fileId, owner.ownerType())) {
            entityManager.remove(existing);
        }

        return bind(fileId, owner);
    }

    /**
     * 🧹 Remove every binding of one file, whatever it is filed against.
     *
     * @param fileId the file
     * @return how many bindings were removed
     */
    public int unbindAll(String fileId) {
        // ⚠️ Through the context, for the same reason refile() is — a bulk DELETE leaves the removed
        // rows managed, and anything that re-binds in the same transaction then reads a ghost.
        List<FileBinding> removed = entityManager.createQuery(
                "SELECT binding FROM FileBinding binding WHERE binding.fileId = :fileId",
                FileBinding.class)
            .setParameter("fileId", fileId)
            .getResultList();

        removed.forEach(entityManager::remove);

        return removed.size();
    }

    /**
     * 🔎 Every binding of one file against owners of a given kind.
     *
     * @param fileId    the file
     * @param ownerType the kind of owner
     * @return the bindings, managed
     */
    private List<FileBinding> matching(String fileId, String ownerType) {
        return entityManager.createQuery(
                "SELECT binding FROM FileBinding binding "
                + "WHERE binding.fileId = :fileId AND binding.ownerType = :ownerType",
                FileBinding.class)
            .setParameter("fileId", fileId)
            .setParameter("ownerType", ownerType)
            .getResultList();
    }

    /**
     * 📂 Everything filed against one owner, in the order somebody put it in.
     *
     * @param owner what holds them
     * @return the bindings
     */
    public List<FileBinding> of(OwnerReference owner) {
        return entityManager.createQuery(
                "SELECT binding FROM FileBinding binding "
                + "WHERE binding.ownerType = :ownerType AND binding.ownerId = :ownerId "
                + "ORDER BY binding.sortOrder, binding.createdAt", FileBinding.class)
            .setParameter("ownerType", owner.ownerType())
            .setParameter("ownerId", owner.ownerId())
            .getResultList();
    }

    /**
     * 🔎 Everywhere one file is filed.
     *
     * @param fileId the file
     * @return its owners
     */
    public List<OwnerReference> ownersOf(String fileId) {
        return entityManager.createQuery(
                "SELECT binding FROM FileBinding binding WHERE binding.fileId = :fileId",
                FileBinding.class)
            .setParameter("fileId", fileId)
            .getResultList()
            .stream()
            .map(FileBinding::owner)
            .toList();
    }

    /**
     * 🔎 The one owner of a given kind this file is filed against, where there is one.
     *
     * @param fileId    the file
     * @param ownerType the kind of owner to look for
     * @return that owner, or empty
     */
    public Optional<OwnerReference> ownerOf(String fileId, String ownerType) {
        return entityManager.createQuery(
                "SELECT binding FROM FileBinding binding "
                + "WHERE binding.fileId = :fileId AND binding.ownerType = :ownerType",
                FileBinding.class)
            .setParameter("fileId", fileId)
            .setParameter("ownerType", ownerType.toUpperCase(java.util.Locale.ROOT))
            .setMaxResults(1)
            .getResultStream()
            .findFirst()
            .map(FileBinding::owner);
    }

    /**
     * 🔢 Where a new file goes among an owner's existing ones: on the end.
     *
     * @param owner what holds them
     * @return the next sort order
     */
    private int nextSortOrder(OwnerReference owner) {
        Integer highest = entityManager.createQuery(
                "SELECT MAX(binding.sortOrder) FROM FileBinding binding "
                + "WHERE binding.ownerType = :ownerType AND binding.ownerId = :ownerId", Integer.class)
            .setParameter("ownerType", owner.ownerType())
            .setParameter("ownerId", owner.ownerId())
            .getSingleResult();

        return highest == null ? 0 : highest + 1;
    }
}
