package org.jmouse.cad.jpa.mount;

import jakarta.persistence.EntityManager;

import java.util.List;
import java.util.Optional;

/**
 * 📁 The folders a curator mounted, as rows of {@code cad_mounted_folders}.
 *
 * <p>It checks nothing — a folder is checked before it is kept — and transaction demarcation is the caller's throughout.</p>
 */
public class MountedFolderRecords {

    private final EntityManager entityManager;

    public MountedFolderRecords(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /** Every mounted folder, by its title. */
    public List<MountedFolder> all() {
        return entityManager.createQuery("select folder from MountedFolder folder order by folder.title asc", MountedFolder.class)
                .getResultList();
    }

    public Optional<MountedFolder> find(String id) {
        return Optional.ofNullable(entityManager.find(MountedFolder.class, id));
    }

    public void keep(MountedFolder folder) {
        entityManager.persist(folder);
    }

    /** Forgets a mounted folder; what it holds on disk is untouched. */
    public void remove(String id) {
        find(id).ifPresent(entityManager::remove);
    }
}
