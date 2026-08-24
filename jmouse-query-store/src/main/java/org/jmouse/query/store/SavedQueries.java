package org.jmouse.query.store;

import org.jmouse.query.store.exception.SavedQueryNotFoundException;

import java.util.List;
import java.util.Optional;

/**
 * 🗄️ Where saved queries are kept.
 *
 * <h2>⚠️ Five methods, and no sixth</h2>
 *
 * <p>Everything a product asks of saved queries is one of these; anything more specific is a
 * {@link SavedQueryCriteria}. That is deliberate — this interface is implemented once per backend, and
 * every method added to it is a method written again in each of them.</p>
 *
 * <h2>⚠️ It checks nothing</h2>
 *
 * <p>Not the jMQ, not the source, not the lengths. A store that validated would validate slightly
 * differently in each implementation, and a query saved through one backend would then be refused by
 * another. Checking is {@link QueryLibrary}'s, once, in front of every store.</p>
 *
 * <p>Transaction demarcation is the caller's throughout, as everywhere else in these libraries.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public interface SavedQueries {

    /**
     * 💾 Keep a new query.
     *
     * @param identifier the identifier to keep it under
     * @param draft      what to keep
     * @return the saved query
     */
    SavedQuery save(String identifier, SavedQueryDraft draft);

    /**
     * ✏️ Replace what a saved query says.
     *
     * <p>⚠️ Its identifier, its source and its owner are not among what may change. A query that moved
     * to another owner or another source is a different query wearing the same name — and anything
     * pointing at it by identifier, a board most of all, would follow the change without being asked.</p>
     *
     * @param identifier which query
     * @param draft      what it should now say
     * @return the updated query
     * @throws SavedQueryNotFoundException when there is no such query
     */
    SavedQuery update(String identifier, SavedQueryDraft draft);

    /**
     * 🔎 One query, if it is there.
     *
     * @param identifier which query
     * @return the query, or empty
     */
    Optional<SavedQuery> find(String identifier);

    /**
     * 📋 Everything answering these criteria, in the order somebody arranged them.
     *
     * @param criteria what to list
     * @return the queries
     */
    List<SavedQuery> list(SavedQueryCriteria criteria);

    /**
     * 🗑️ Forget one.
     *
     * @param identifier which query
     * @return whether there had been one
     */
    boolean remove(String identifier);

    /**
     * 🔎 One query, or a refusal naming it.
     *
     * @param identifier which query
     * @return the query
     * @throws SavedQueryNotFoundException when there is no such query
     */
    default SavedQuery require(String identifier) {
        return find(identifier).orElseThrow(() -> new SavedQueryNotFoundException(identifier));
    }
}
