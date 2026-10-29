package org.jmouse.cad.jpa.catalogue;

import jakarta.persistence.EntityManager;

import java.util.List;
import java.util.Optional;

/**
 * 📚 The catalogue's sections.
 *
 * <p>It does not authorize anything, and transaction demarcation is the caller's throughout.</p>
 */
public class CategoryStore {

    private final EntityManager entityManager;

    /**
     * 🏗️ Work over the application's persistence context.
     *
     * @param entityManager the persistence context
     */
    public CategoryStore(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /**
     * 📚 Every section, in the order they are shown.
     *
     * @return the sections
     */
    public List<DocumentCategory> allInOrder() {
        return entityManager.createQuery(
                        "select category from DocumentCategory category order by category.position asc",
                        DocumentCategory.class)
                .getResultList();
    }

    /**
     * 🔎 One section.
     *
     * @param code its code
     * @return the section, or empty
     */
    public Optional<DocumentCategory> find(String code) {
        return Optional.ofNullable(entityManager.find(DocumentCategory.class, code));
    }

    /**
     * ❓ Whether a section exists.
     *
     * @param code its code
     * @return whether it does
     */
    public boolean exists(String code) {
        return find(code).isPresent();
    }

    /**
     * 💾 Keep a section: a new one is persisted, a loaded one is already tracked.
     *
     * @param category the section
     * @return the same section, now managed
     */
    public DocumentCategory save(DocumentCategory category) {
        if (!entityManager.contains(category)) {
            entityManager.persist(category);
        }

        return category;
    }

    /**
     * 🚿 Write what the persistence context holds now.
     */
    public void flush() {
        entityManager.flush();
    }

    /**
     * 🗑️ Remove a section, if it is there.
     *
     * @param code its code
     */
    public void delete(String code) {
        find(code).ifPresent(entityManager::remove);
    }
}
