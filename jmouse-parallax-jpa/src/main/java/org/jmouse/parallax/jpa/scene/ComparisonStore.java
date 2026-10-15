package org.jmouse.parallax.jpa.scene;

import jakarta.persistence.EntityManager;
import org.jmouse.parallax.jpa.NativeValues;
import org.jmouse.parallax.owner.ParallaxOwner;

import java.util.List;
import java.util.Optional;

/**
 * 🪟 Comparisons and their slots.
 *
 * <h3>⚠️ The slots are written by native statements, and that orders the writes</h3>
 *
 * <p>{@code comparison_slots} has no entity: the slots are rewritten whole on every save. A comparison persisted in
 * the same transaction may still sit in the persistence context when they are, so {@link #replaceSlots} flushes
 * first.</p>
 *
 * <p>It does not authorize anything, and transaction demarcation is the caller's throughout.</p>
 */
public class ComparisonStore {

    private final EntityManager entityManager;

    /**
     * 🏗️ Work over the application's persistence context.
     *
     * @param entityManager the persistence context
     */
    public ComparisonStore(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /**
     * 🔎 One comparison.
     *
     * @param comparisonId the comparison
     * @return the comparison, or empty
     */
    public Optional<Comparison> find(String comparisonId) {
        return Optional.ofNullable(entityManager.find(Comparison.class, comparisonId));
    }

    /**
     * 📂 One owner's comparisons, most recently changed first.
     *
     * @param owner the owner (a project)
     * @return the comparisons
     */
    public List<Comparison> owned(ParallaxOwner owner) {
        return entityManager.createQuery("""
                        select comparison from Comparison comparison
                        where comparison.ownerType = :type and comparison.ownerId = :id
                        order by comparison.updatedAt desc
                        """, Comparison.class)
                .setParameter("type", owner.type())
                .setParameter("id", owner.id())
                .getResultList();
    }

    /**
     * 💾 Keep a comparison: a new one is persisted, a loaded one is already tracked.
     *
     * @param comparison the comparison
     * @return the same comparison, now managed
     */
    public Comparison save(Comparison comparison) {
        if (!entityManager.contains(comparison)) {
            entityManager.persist(comparison);
        }

        return comparison;
    }

    /**
     * 🗑️ Remove a comparison; its slots go with it (the schema cascades).
     *
     * @param comparison the comparison
     */
    public void remove(Comparison comparison) {
        entityManager.remove(comparison);
    }

    /**
     * 🪟 A comparison's slots, in their order.
     *
     * @param comparisonId the comparison
     * @return the slots
     */
    public List<ComparisonSlot> slots(String comparisonId) {
        List<?> rows = entityManager.createNativeQuery("""
                        SELECT label, object_id, inline_source, representation_kind, settings, parameter_values, camera
                        FROM comparison_slots WHERE comparison_id = :comparisonId ORDER BY position
                        """)
                .setParameter("comparisonId", comparisonId)
                .getResultList();

        return rows.stream()
                .map(Object[].class::cast)
                .map(columns -> new ComparisonSlot(NativeValues.stringOf(columns[0]),
                                                   NativeValues.stringOf(columns[1]),
                                                   NativeValues.stringOf(columns[2]),
                                                   NativeValues.stringOf(columns[3]),
                                                   NativeValues.stringOf(columns[4]),
                                                   NativeValues.stringOf(columns[5]),
                                                   NativeValues.stringOf(columns[6])))
                .toList();
    }

    /**
     * 🔁 Rewrite a comparison's slots, in the order given.
     *
     * <p>⚠️ Flushes first: the comparison must reach the database before its slots point at it.</p>
     *
     * @param comparisonId the comparison
     * @param slots        the slots
     */
    public void replaceSlots(String comparisonId, List<ComparisonSlot> slots) {
        entityManager.flush();
        entityManager.createNativeQuery("DELETE FROM comparison_slots WHERE comparison_id = :comparisonId")
                .setParameter("comparisonId", comparisonId)
                .executeUpdate();

        for (int position = 0; position < slots.size(); position++) {
            ComparisonSlot slot = slots.get(position);

            entityManager.createNativeQuery("""
                            INSERT INTO comparison_slots (comparison_id, position, label, object_id, inline_source,
                                                          representation_kind, settings, parameter_values, camera)
                            VALUES (:comparisonId, :position, :label, :objectId, :inlineSource, :representationKind,
                                    :settings, :parameterValues, :camera)
                            """)
                    .setParameter("comparisonId", comparisonId)
                    .setParameter("position", position)
                    .setParameter("label", slot.label())
                    .setParameter("objectId", slot.objectId())
                    .setParameter("inlineSource", slot.inlineSource())
                    .setParameter("representationKind", slot.representationKind())
                    .setParameter("settings", slot.settings())
                    .setParameter("parameterValues", slot.parameterValues())
                    .setParameter("camera", slot.camera())
                    .executeUpdate();
        }
    }
}
