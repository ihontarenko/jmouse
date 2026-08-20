package org.jmouse.storage.jpa;

import jakarta.persistence.EntityManager;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 🔎 Who points at the registry, asked of the mappings rather than of a person.
 *
 * <h3>⚠️ The mistake this exists to make impossible</h3>
 *
 * <p>The sweeper reclaims every object that appears in <strong>no</strong> declared reference source.
 * So a product that adds a second kind of file and forgets to declare the table holding it has not
 * made a configuration mistake — it has armed a scheduled job to delete live bytes. Every product
 * carried a comment saying exactly that, in capitals, which is the clearest possible sign that a
 * comment was the wrong mechanism: the warning is only ever read by somebody already editing the
 * file they were supposed to remember to edit.</p>
 *
 * <p>And the delay makes it worse. The sweeper ships disabled, so the omission keeps quietly until
 * somebody turns it on — by which time the change that introduced it is months old and nobody
 * connects the two.</p>
 *
 * <p>Every mapping that points at the registry is a {@code @ManyToOne} or {@code @OneToOne} to
 * {@link StoredFile}, and the persistence metamodel already knows all of them. Asking it cannot
 * forget.</p>
 *
 * <h3>What it finds, and what it does not</h3>
 *
 * <p><strong>Singular associations only.</strong> A collection of stored files, or a reference held
 * as a bare identifier string rather than as an association, is invisible here — the first because
 * nothing in these products has one, the second because it is not a mapping at all. Both are still
 * declarable by hand, which is why manual sources are combined with these rather than replaced by
 * them.</p>
 *
 * <p><strong>{@code IS NOT NULL} follows the mapping.</strong> ⚠️ Note that {@code optional} defaults
 * to {@code true} on {@code @ManyToOne} even where the join column is {@code NOT NULL}, so the guard
 * is often added where it is not needed. That is the harmless direction: a redundant condition costs
 * nothing, and a missing one would drop rows whose column is genuinely nullable — which is to say it
 * would report fewer references than exist, which is the direction that deletes data.</p>
 */
public final class StoredFileReferenceDiscovery {

    private static final Logger LOGGER = LoggerFactory.getLogger(StoredFileReferenceDiscovery.class);

    /** Alias for the owning entity in every generated query. */
    private static final String OWNER_ALIAS = "owner";

    private StoredFileReferenceDiscovery() {
    }

    /**
     * 🔎 Every mapped association pointing at the registry, as a reference source each.
     *
     * @param entityManager persistence context whose metamodel is asked, and which the sources query
     * @return one source per discovered association, ordered by name so a log line is stable
     */
    public static List<StoredFileReferences> discover(EntityManager entityManager) {
        EntityType<StoredFile> registryType = entityManager.getMetamodel().entity(StoredFile.class);
        String                 identifierName = registryType.getId(String.class).getName();

        List<StoredFileReferences> discovered = new ArrayList<>();

        for (EntityType<?> entityType : entityManager.getMetamodel().getEntities()) {
            if (entityType.getJavaType() == StoredFile.class) {
                continue;
            }

            for (SingularAttribute<?, ?> attribute : entityType.getSingularAttributes()) {
                if (!attribute.isAssociation() || attribute.getJavaType() != StoredFile.class) {
                    continue;
                }

                discovered.add(sourceFor(entityManager, entityType, attribute, identifierName));
            }
        }

        discovered.sort(Comparator.comparing(StoredFileReferences::sourceName));

        return List.copyOf(discovered);
    }

    /**
     * 🔗 One association, as the query that answers it.
     *
     * @param entityManager  persistence context the source queries
     * @param entityType     the entity holding the association
     * @param attribute      the association itself
     * @param identifierName name of the registry's identifier attribute
     * @return the reference source
     */
    private static StoredFileReferences sourceFor(EntityManager entityManager, EntityType<?> entityType,
                                                  SingularAttribute<?, ?> attribute, String identifierName) {
        String sourceName = "%s.%s".formatted(entityType.getName(), attribute.getName());
        String query      = "SELECT %s.%s.%s FROM %s %s"
            .formatted(OWNER_ALIAS, attribute.getName(), identifierName, entityType.getName(), OWNER_ALIAS);

        if (attribute.isOptional()) {
            query += " WHERE %s.%s IS NOT NULL".formatted(OWNER_ALIAS, attribute.getName());
        }

        ensureParses(entityManager, sourceName, query);

        LOGGER.debug("🔎 Discovered stored-file reference '{}' — {}", sourceName, query);

        return new JpaStoredFileReferences(entityManager, sourceName, query);
    }

    /**
     * ✅ Parse the generated query now, so a bad one is a failure to start.
     *
     * <p>⚠️ <strong>The whole point of the timing.</strong> A JPQL string is only parsed when it is
     * first created, and these are created inside a sweep — which runs on a schedule, at night, in a
     * job whose failure nobody is watching. A query built wrong would therefore surface as a sweep
     * that quietly stopped working, which is indistinguishable from a sweep that found nothing to do.
     * Parsing here converts that into a context that refuses to come up.</p>
     *
     * @param entityManager persistence context that parses it
     * @param sourceName    what to name in the failure
     * @param query         the generated query
     */
    private static void ensureParses(EntityManager entityManager, String sourceName, String query) {
        try {
            entityManager.createQuery(query, String.class);
        } catch (RuntimeException malformed) {
            throw new IllegalStateException(
                "Discovered stored-file reference '%s' produced a query the persistence provider "
                + "rejected: %s".formatted(sourceName, query), malformed);
        }
    }
}
