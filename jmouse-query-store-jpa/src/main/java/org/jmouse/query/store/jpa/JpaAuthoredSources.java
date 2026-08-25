package org.jmouse.query.store.jpa;

import jakarta.persistence.EntityManager;
import org.jmouse.query.store.AuthoredSource;
import org.jmouse.query.store.AuthoredSources;
import org.jmouse.query.store.QueryOwner;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 🗄️ Authored declarations kept in the database.
 *
 * <h2>⚠️ It stores text and validates nothing</h2>
 *
 * <p>Whether a body parses, whether its mapping names a table this installation publishes, and whether
 * the caller may write it at all are answered in front of this class — by the language, by the
 * allow-list and by the subject. A store that also had an opinion would be a fourth one, and the fourth
 * is what nobody remembers to update.</p>
 *
 * <p>Transaction demarcation is the caller's.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class JpaAuthoredSources implements AuthoredSources {

    private final EntityManager entityManager;

    /**
     * 🏗️ Work over the application's persistence context.
     *
     * @param entityManager the persistence context
     */
    public JpaAuthoredSources(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Optional<AuthoredSource> find(QueryOwner owner, String sourceKey) {
        return row(owner, sourceKey).map(QuerySourceRow::toReference);
    }

    /**
     * ⚠️ An upsert, because the table allows exactly one declaration per owner and source.
     *
     * <p>Offering {@code create} and {@code update} separately would push that fact onto every caller —
     * and the caller that guessed wrong would get a constraint violation rather than the row it asked
     * for. There is one declaration; writing it is one operation.</p>
     */
    @Override
    public AuthoredSource save(AuthoredSource source) {
        QuerySourceRow row = row(source.owner(), source.sourceKey())
                .orElseGet(() -> new QuerySourceRow(
                        UUID.randomUUID().toString(), source.sourceKey(), source.owner()));

        // ⚠️ WRITTEN BEFORE IT IS PERSISTED, and the order is not cosmetic. Persisting first and filling
        // the fields afterwards left the insert to be assembled from whatever the entity held at flush —
        // and with an assigned identifier Hibernate is free to issue it early, which it did: the very
        // first save died on `author cannot be null` about a value the next line was about to set.
        row.write(source.body(), source.author());

        if (!entityManager.contains(row)) {
            entityManager.persist(row);
        }

        // ⚠️ So the stamp the caller is handed back is the one the database will hold, rather than the
        // one the entity had a moment before @PreUpdate ran.
        entityManager.flush();

        return row.toReference();
    }

    @Override
    public boolean remove(QueryOwner owner, String sourceKey) {
        return row(owner, sourceKey)
                .map(row -> {
                    entityManager.remove(row);
                    return true;
                })
                .orElse(false);
    }

    @Override
    public List<AuthoredSource> list(QueryOwner owner) {
        return entityManager
                .createQuery("""
                        SELECT source FROM QuerySourceRow source
                        WHERE source.owner.type = :type AND source.owner.identifier = :identifier
                        ORDER BY source.sourceKey
                        """, QuerySourceRow.class)
                .setParameter("type", owner.type())
                .setParameter("identifier", owner.identifier())
                .getResultList()
                .stream()
                .map(QuerySourceRow::toReference)
                .toList();
    }

    private Optional<QuerySourceRow> row(QueryOwner owner, String sourceKey) {
        return entityManager
                .createQuery("""
                        SELECT source FROM QuerySourceRow source
                        WHERE source.owner.type = :type
                          AND source.owner.identifier = :identifier
                          AND source.sourceKey = :sourceKey
                        """, QuerySourceRow.class)
                .setParameter("type", owner.type())
                .setParameter("identifier", owner.identifier())
                .setParameter("sourceKey", sourceKey)
                .getResultStream()
                .findFirst();
    }
}
