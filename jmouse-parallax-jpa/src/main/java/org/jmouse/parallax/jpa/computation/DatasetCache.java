package org.jmouse.parallax.jpa.computation;

import jakarta.persistence.EntityManager;
import org.jmouse.parallax.jpa.NativeValues;

import java.time.Instant;
import java.util.Optional;

/**
 * 💾 The computed datasets, kept by the fingerprint of everything that determined them.
 *
 * <p>⚠️ Every query reads a list, never a stream: outside a transaction the shared proxy closes its
 * {@code EntityManager} as the call returns, and a stream read after that finds its result set closed.</p>
 *
 * <p>It does not authorize anything, and transaction demarcation is the caller's throughout.</p>
 */
public class DatasetCache {

    private final EntityManager entityManager;

    /**
     * 🏗️ Work over the application's persistence context.
     *
     * @param entityManager the persistence context
     */
    public DatasetCache(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /**
     * 📊 How much the cache holds.
     *
     * @param datasets how many datasets
     * @param bytes    their payloads' size before compression, summed
     */
    public record Totals(long datasets, long bytes) {
    }

    /**
     * 🔎 One dataset by its identifier.
     *
     * @param datasetId the dataset
     * @return the dataset, or empty
     */
    public Optional<ComputedDataset> find(String datasetId) {
        return Optional.ofNullable(entityManager.find(ComputedDataset.class, datasetId));
    }

    /**
     * 🔎 The dataset kept under a fingerprint.
     *
     * @param fingerprint the fingerprint
     * @return the dataset, or empty
     */
    public Optional<ComputedDataset> findByFingerprint(String fingerprint) {
        return entityManager.createQuery(
                        "select dataset from ComputedDataset dataset where dataset.fingerprint = :fingerprint",
                        ComputedDataset.class)
                .setParameter("fingerprint", fingerprint)
                .getResultList()
                .stream()
                .findFirst();
    }

    /**
     * ❓ Whether a dataset is kept under a fingerprint.
     *
     * @param fingerprint the fingerprint
     * @return whether one is
     */
    public boolean contains(String fingerprint) {
        return entityManager.createQuery(
                        "select count(dataset) from ComputedDataset dataset where dataset.fingerprint = :fingerprint",
                        Long.class)
                .setParameter("fingerprint", fingerprint)
                .getSingleResult() > 0;
    }

    /**
     * 💾 Keep a new dataset and write it at once.
     *
     * <p>Flushed here so that a second process that computed the same fingerprint fails now, on the unique
     * fingerprint, where the caller can take the dataset already kept instead.</p>
     *
     * @param dataset the dataset
     * @return the same dataset, now managed
     */
    public ComputedDataset store(ComputedDataset dataset) {
        entityManager.persist(dataset);
        entityManager.flush();

        return dataset;
    }

    /**
     * 👆 Record that a dataset was asked for again.
     *
     * @param datasetId the dataset
     * @param now       when
     */
    public void recordUse(String datasetId, Instant now) {
        entityManager.createQuery("""
                        update ComputedDataset dataset
                        set dataset.lastUsedAt = :now, dataset.useCount = dataset.useCount + 1
                        where dataset.id = :id
                        """)
                .setParameter("now", now)
                .setParameter("id", datasetId)
                .executeUpdate();
    }

    /**
     * 🧹 Forget every dataset nobody has asked for since a moment.
     *
     * @param before the moment
     * @return how many were forgotten
     */
    public int forgetUnusedSince(Instant before) {
        return entityManager.createQuery("delete from ComputedDataset dataset where dataset.lastUsedAt < :before")
                .setParameter("before", before)
                .executeUpdate();
    }

    /**
     * 📊 How much the cache holds.
     *
     * @return the totals
     */
    public Totals totals() {
        Object[] columns = (Object[]) entityManager.createNativeQuery(
                        "SELECT COUNT(*), COALESCE(SUM(payload_bytes), 0) FROM computed_datasets")
                .getSingleResult();

        return new Totals(NativeValues.longOf(columns[0]), NativeValues.longOf(columns[1]));
    }
}
