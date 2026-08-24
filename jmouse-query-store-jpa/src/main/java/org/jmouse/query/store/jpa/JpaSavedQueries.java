package org.jmouse.query.store.jpa;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.jmouse.query.store.QueryOwner;
import org.jmouse.query.store.SavedQueries;
import org.jmouse.query.store.SavedQuery;
import org.jmouse.query.store.SavedQueryCriteria;
import org.jmouse.query.store.SavedQueryDraft;
import org.jmouse.query.store.exception.SavedQueryNotFoundException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 🗄️ Saved queries kept in the database.
 *
 * <h2>⚠️ The listing is pushed down, and has to keep agreeing with the one in memory</h2>
 *
 * <p>Assembling the same criteria in JPQL rather than filtering everything in Java is the difference
 * between a screen that opens and a screen that reads every saved query in the installation to show
 * four. But it means the rule now exists twice, so the query below is written to match
 * {@code SavedQueryCriteria#matches} clause for clause — including {@code LOWER} on the name, because
 * one engine sorts case-insensitively by collation and the other does not, and a list that reorders
 * itself when a product changes database is the kind of thing nobody reports.</p>
 *
 * <p>⚠️ It checks nothing about the jMQ. That belongs in front of every store, once, so a query saved
 * through one backend is not refused by another.</p>
 *
 * <p>Transaction demarcation is the caller's.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class JpaSavedQueries implements SavedQueries {

    private final EntityManager entityManager;

    /**
     * 🏗️ Work over the application's persistence context.
     *
     * @param entityManager the persistence context
     */
    public JpaSavedQueries(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public SavedQuery save(String identifier, SavedQueryDraft draft) {
        SavedQueryRow row = new SavedQueryRow(identifier, draft);

        entityManager.persist(row);

        return row;
    }

    @Override
    public SavedQuery update(String identifier, SavedQueryDraft draft) {
        SavedQueryRow row = entityManager.find(SavedQueryRow.class, identifier);

        if (row == null) {
            throw new SavedQueryNotFoundException(identifier);
        }

        row.rewriteWith(draft);

        return row;
    }

    @Override
    public Optional<SavedQuery> find(String identifier) {
        return Optional.ofNullable(entityManager.find(SavedQueryRow.class, identifier));
    }

    @Override
    public List<SavedQuery> list(SavedQueryCriteria criteria) {
        List<String>        conditions = new ArrayList<>();
        Map<String, Object> values     = new LinkedHashMap<>();

        criteria.ownerReference().ifPresent(owner -> {
            conditions.add("query.owner.type = :ownerType AND query.owner.identifier = :ownerId");
            values.put("ownerType", owner.type());
            values.put("ownerId", owner.identifier());
        });

        if (criteria.source() != null) {
            conditions.add("query.source = :source");
            values.put("source", criteria.source());
        }

        // ⚠️ Not authorization — it keeps a person's own unshared queries out of somebody else's list,
        // and says nothing about whether the reader may reach the owner. That is gated at the route.
        if (criteria.reader() != null) {
            conditions.add("(query.shared = TRUE OR query.author = :reader)");
            values.put("reader", criteria.reader());
        } else {
            conditions.add("query.shared = TRUE");
        }

        String statement = "SELECT query FROM SavedQueryRow query WHERE " + String.join(" AND ", conditions)
                           + " ORDER BY query.sortOrder, LOWER(query.name)";

        TypedQuery<SavedQueryRow> selection = entityManager.createQuery(statement, SavedQueryRow.class);

        values.forEach(selection::setParameter);

        return List.copyOf(selection.getResultList());
    }

    @Override
    public boolean remove(String identifier) {
        SavedQueryRow row = entityManager.find(SavedQueryRow.class, identifier);

        if (row == null) {
            return false;
        }

        entityManager.remove(row);

        return true;
    }

    /**
     * 🧹 Forget everything one owner held.
     *
     * <p>⚠️ Beyond the contract on purpose, and the one thing a product genuinely cannot write itself
     * without reaching past this class: a deleted board leaves its filters behind, and rows whose owner
     * no longer exists are invisible to every screen while still being listed by every count.</p>
     *
     * @param owner what was removed
     * @return how many queries went with it
     */
    public int removeEverythingOwnedBy(QueryOwner owner) {
        return entityManager.createQuery(
                        "DELETE FROM SavedQueryRow query "
                        + "WHERE query.owner.type = :ownerType AND query.owner.identifier = :ownerId")
                .setParameter("ownerType", owner.type())
                .setParameter("ownerId", owner.identifier())
                .executeUpdate();
    }
}
