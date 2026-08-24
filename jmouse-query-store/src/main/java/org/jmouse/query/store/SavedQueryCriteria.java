package org.jmouse.query.store;

import java.util.Optional;

/**
 * 🔍 What to list.
 *
 * <h2>⚠️ One shape for every listing, rather than a method per question</h2>
 *
 * <p>The interface would otherwise grow the same family in every implementation —
 * {@code listFor(owner)}, {@code listFor(owner, source)}, {@code listVisibleTo(owner, reader)} — and
 * each new question would be a method added to the contract and then written again everywhere behind
 * it. Here a store answers one question, and a caller narrows it.</p>
 *
 * <h2>⚠️ {@code reader} is not authorization</h2>
 *
 * <p>It keeps a person's own unshared queries out of somebody else's list. It says nothing about
 * whether the reader may reach the board those queries hang off — every product in this workspace gates
 * that at the route through one engine, and a second opinion here would be a rule to keep in step.</p>
 *
 * @param owner  what holds them, or {@code null} for every owner
 * @param source which described source they are written against, or {@code null} for all of them
 * @param reader who is looking, or {@code null} to list shared queries only
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public record SavedQueryCriteria(QueryOwner owner, String source, String reader) {

    /**
     * 🏷️ Everything belonging to one owner.
     *
     * @param owner what holds them
     * @return the criteria
     */
    public static SavedQueryCriteria ownedBy(QueryOwner owner) {
        return new SavedQueryCriteria(owner, null, null);
    }

    /**
     * 🌍 Everything, of every owner.
     *
     * @return the criteria
     */
    public static SavedQueryCriteria everything() {
        return new SavedQueryCriteria(null, null, null);
    }

    /**
     * 🎯 Narrow to one described source.
     *
     * @param source the source name
     * @return the criteria, narrowed
     */
    public SavedQueryCriteria on(String source) {
        return new SavedQueryCriteria(owner, source, reader);
    }

    /**
     * 👤 Narrow to what this person may see — shared queries, plus their own.
     *
     * @param reader the product's identifier for the person looking
     * @return the criteria, narrowed
     */
    public SavedQueryCriteria seenBy(String reader) {
        return new SavedQueryCriteria(owner, source, reader);
    }

    /**
     * Whether one saved query answers these criteria.
     *
     * <p>⚠️ Here rather than in each store, so a listing out of a map and a listing out of a database
     * cannot come to mean different things. A store able to push the same question down to its engine is
     * welcome to; this is what it has to agree with.</p>
     *
     * @param query the candidate
     * @return whether it belongs in the answer
     */
    public boolean matches(SavedQuery query) {
        if (owner != null && !owner.equals(query.getOwner())) {
            return false;
        }

        if (source != null && !source.equals(query.getSource())) {
            return false;
        }

        return query.isShared() || (reader != null && reader.equals(query.getAuthor()));
    }

    /**
     * The owner, where one was named.
     *
     * @return the owner
     */
    public Optional<QueryOwner> ownerReference() {
        return Optional.ofNullable(owner);
    }
}
