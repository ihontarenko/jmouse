package org.jmouse.parallax.jpa.collection;

import jakarta.persistence.EntityManager;
import org.jmouse.parallax.jpa.NativeValues;
import org.jmouse.parallax.owner.ParallaxOwner;

import java.util.List;
import java.util.Optional;

/**
 * 🗂️ Collections and their items.
 *
 * <h3>⚠️ The items are written by native statements, and that orders the writes</h3>
 *
 * <p>{@code collection_items} has no entity: an item is a row of references read into a {@link CollectionItem}. A
 * collection persisted in the same transaction may still sit in the persistence context when its first item is
 * added, so {@link #addItem} flushes first.</p>
 *
 * <p>⚠️ Every query reads a list, never a stream: outside a transaction the shared proxy closes its
 * {@code EntityManager} as the call returns, and a stream read after that finds its result set closed.</p>
 *
 * <p>It does not authorize anything, and transaction demarcation is the caller's throughout.</p>
 */
public class CollectionStore {

    private final EntityManager entityManager;

    /**
     * 🏗️ Work over the application's persistence context.
     *
     * @param entityManager the persistence context
     */
    public CollectionStore(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    // ── Collections ──────────────────────────────────────────────────────────────────────────────────────────────

    /**
     * 🔎 One collection by its identifier.
     *
     * @param collectionId the collection
     * @return the collection, or empty
     */
    public Optional<ObjectCollection> find(String collectionId) {
        return Optional.ofNullable(entityManager.find(ObjectCollection.class, collectionId));
    }

    /**
     * 🔎 One collection by its slug.
     *
     * @param slug the slug
     * @return the collection, or empty
     */
    public Optional<ObjectCollection> findBySlug(String slug) {
        return entityManager.createQuery(
                        "select collection from ObjectCollection collection where collection.slug = :slug",
                        ObjectCollection.class)
                .setParameter("slug", slug)
                .getResultList()
                .stream()
                .findFirst();
    }

    /**
     * ❓ Whether a slug is taken.
     *
     * @param slug the slug
     * @return whether a collection has it
     */
    public boolean existsBySlug(String slug) {
        return entityManager.createQuery(
                        "select count(collection) from ObjectCollection collection where collection.slug = :slug",
                        Long.class)
                .setParameter("slug", slug)
                .getSingleResult() > 0;
    }

    /**
     * 📚 The collections of one kind and purpose, in their order.
     *
     * @param kind    curated or personal
     * @param purpose shelf or formula set
     * @return the collections
     */
    public List<ObjectCollection> ofKind(ObjectCollection.Kind kind, ObjectCollection.Purpose purpose) {
        return entityManager.createQuery("""
                        select collection from ObjectCollection collection
                        where collection.kind = :kind and collection.purpose = :purpose
                        order by collection.position asc
                        """, ObjectCollection.class)
                .setParameter("kind", kind)
                .setParameter("purpose", purpose)
                .getResultList();
    }

    /**
     * 📂 One owner's collections, most recently changed first.
     *
     * @param owner the owner (an account)
     * @return the collections
     */
    public List<ObjectCollection> owned(ParallaxOwner owner) {
        return entityManager.createQuery("""
                        select collection from ObjectCollection collection
                        where collection.ownerType = :type and collection.ownerId = :id
                        order by collection.updatedAt desc
                        """, ObjectCollection.class)
                .setParameter("type", owner.type())
                .setParameter("id", owner.id())
                .getResultList();
    }

    /**
     * 💾 Keep a collection: a new one is persisted, a loaded one is already tracked.
     *
     * @param collection the collection
     * @return the same collection, now managed
     */
    public ObjectCollection save(ObjectCollection collection) {
        if (!entityManager.contains(collection)) {
            entityManager.persist(collection);
        }

        return collection;
    }

    /**
     * 🗑️ Remove a collection; its items go with it (the schema cascades).
     *
     * @param collection the collection
     */
    public void remove(ObjectCollection collection) {
        entityManager.remove(collection);
    }

    // ── Items ────────────────────────────────────────────────────────────────────────────────────────────────────

    /**
     * 📌 A collection's items, in their order.
     *
     * @param collectionId the collection
     * @return the items
     */
    public List<CollectionItem> items(String collectionId) {
        List<?> rows = entityManager.createNativeQuery("""
                        SELECT id, position, item_kind, object_id, scene_id, comparison_id, note, section, added_by,
                               added_at
                        FROM collection_items WHERE collection_id = :collectionId ORDER BY position
                        """)
                .setParameter("collectionId", collectionId)
                .getResultList();

        return rows.stream()
                .map(Object[].class::cast)
                .map(columns -> new CollectionItem(NativeValues.stringOf(columns[0]),
                                                   NativeValues.intOf(columns[1]),
                                                   CollectionItem.Kind.valueOf(NativeValues.stringOf(columns[2])),
                                                   NativeValues.stringOf(columns[3]),
                                                   NativeValues.stringOf(columns[4]),
                                                   NativeValues.stringOf(columns[5]),
                                                   NativeValues.stringOf(columns[6]),
                                                   NativeValues.stringOf(columns[7]),
                                                   NativeValues.stringOf(columns[8]),
                                                   NativeValues.instantOf(columns[9])))
                .toList();
    }

    /**
     * 🔢 The identifiers of a collection's items, in their order.
     *
     * @param collectionId the collection
     * @return the identifiers
     */
    public List<String> itemIds(String collectionId) {
        List<?> rows = entityManager.createNativeQuery(
                        "SELECT id FROM collection_items WHERE collection_id = :collectionId ORDER BY position")
                .setParameter("collectionId", collectionId)
                .getResultList();

        return rows.stream().map(NativeValues::stringOf).toList();
    }

    /**
     * 🔢 How many items a collection holds.
     *
     * @param collectionId the collection
     * @return the count
     */
    public long countItems(String collectionId) {
        return NativeValues.longOf(entityManager.createNativeQuery(
                        "SELECT COUNT(*) FROM collection_items WHERE collection_id = :collectionId")
                .setParameter("collectionId", collectionId)
                .getSingleResult());
    }

    /**
     * ⏭️ The position an item added at the end takes.
     *
     * @param collectionId the collection
     * @return the next position
     */
    public int nextItemPosition(String collectionId) {
        return NativeValues.intOf(entityManager.createNativeQuery(
                        "SELECT COALESCE(MAX(position), -1) + 1 FROM collection_items WHERE collection_id = :collectionId")
                .setParameter("collectionId", collectionId)
                .getSingleResult());
    }

    /**
     * ➕ Add an item to a collection.
     *
     * <p>⚠️ Flushes first: the collection may have been persisted in this transaction and not written yet.</p>
     *
     * @param collectionId the collection
     * @param item         the item, its position already chosen
     */
    public void addItem(String collectionId, CollectionItem item) {
        entityManager.flush();
        entityManager.createNativeQuery("""
                        INSERT INTO collection_items (id, collection_id, position, item_kind, section, object_id, scene_id,
                                                      comparison_id, note, added_by, added_at)
                        VALUES (:id, :collectionId, :position, :kind, :section, :objectId, :sceneId, :comparisonId,
                                :note, :addedBy, :addedAt)
                        """)
                .setParameter("id", item.id())
                .setParameter("collectionId", collectionId)
                .setParameter("position", item.position())
                .setParameter("kind", item.kind().name())
                .setParameter("section", item.section())
                .setParameter("objectId", item.objectId())
                .setParameter("sceneId", item.sceneId())
                .setParameter("comparisonId", item.comparisonId())
                .setParameter("note", item.note())
                .setParameter("addedBy", item.addedBy())
                .setParameter("addedAt", item.addedAt())
                .executeUpdate();
    }

    /**
     * ➖ Remove one item of a collection.
     *
     * @param collectionId the collection
     * @param itemId       the item
     * @return whether it was there
     */
    public boolean removeItem(String collectionId, String itemId) {
        return entityManager.createNativeQuery(
                        "DELETE FROM collection_items WHERE id = :itemId AND collection_id = :collectionId")
                .setParameter("itemId", itemId)
                .setParameter("collectionId", collectionId)
                .executeUpdate() > 0;
    }

    /**
     * 🔀 Number the items in the order given, from zero.
     *
     * <p>Two passes, so the unique {@code (collection, position)} index never sees two items on one position.</p>
     *
     * @param itemIds the items, in their new order
     */
    public void renumber(List<String> itemIds) {
        for (int index = 0; index < itemIds.size(); index++) {
            writePosition(itemIds.get(index), -(index + 1));
        }

        for (int index = 0; index < itemIds.size(); index++) {
            writePosition(itemIds.get(index), index);
        }
    }

    private void writePosition(String itemId, int position) {
        entityManager.createNativeQuery("UPDATE collection_items SET position = :position WHERE id = :itemId")
                .setParameter("position", position)
                .setParameter("itemId", itemId)
                .executeUpdate();
    }
}
