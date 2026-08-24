package org.jmouse.query.store.memory;

import org.jmouse.query.store.QueryOwner;
import org.jmouse.query.store.SavedQueries;
import org.jmouse.query.store.SavedQuery;
import org.jmouse.query.store.SavedQueryCriteria;
import org.jmouse.query.store.SavedQueryDraft;
import org.jmouse.query.store.exception.SavedQueryNotFoundException;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 🗃️ Saved queries kept in a map.
 *
 * <h2>⚠️ Not a stub — the second implementation that keeps the first honest</h2>
 *
 * <p>The same reason the query engine has an in-memory backend beside its SQL one: a contract with one
 * implementation is not a contract, it is that implementation's behaviour written down twice. Two of
 * them, answering the same listing identically, is what makes {@link SavedQueryCriteria} a rule rather
 * than a description of whatever the database happened to do.</p>
 *
 * <p>It is also what lets everything above the store — a builder, an editor, a screen, a demonstration
 * — be exercised with no database in sight.</p>
 *
 * <p>⚠️ Not synchronised, and deliberately so. It is a test double and a demonstration; a store that
 * several threads write is a store with a database behind it.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class MemorySavedQueries implements SavedQueries {

    private final Map<String, MemorySavedQuery> queries = new LinkedHashMap<>();

    @Override
    public SavedQuery save(String identifier, SavedQueryDraft draft) {
        LocalDateTime      moment = LocalDateTime.now();
        MemorySavedQuery   query  = new MemorySavedQuery(identifier, draft, moment, moment);

        queries.put(identifier, query);

        return query;
    }

    @Override
    public SavedQuery update(String identifier, SavedQueryDraft draft) {
        MemorySavedQuery existing = queries.get(identifier);

        if (existing == null) {
            throw new SavedQueryNotFoundException(identifier);
        }

        MemorySavedQuery updated = new MemorySavedQuery(
                identifier, existing.rewrittenWith(draft), existing.getCreatedAt(), LocalDateTime.now());

        queries.put(identifier, updated);

        return updated;
    }

    @Override
    public Optional<SavedQuery> find(String identifier) {
        return Optional.ofNullable(queries.get(identifier));
    }

    @Override
    public List<SavedQuery> list(SavedQueryCriteria criteria) {
        return queries.values().stream()
                .filter(criteria::matches)
                .sorted(Comparator.comparingInt(SavedQuery::getSortOrder)
                                .thenComparing(SavedQuery::getName, String.CASE_INSENSITIVE_ORDER))
                .map(SavedQuery.class::cast)
                .toList();
    }

    @Override
    public boolean remove(String identifier) {
        return queries.remove(identifier) != null;
    }

    /**
     * A saved query held as a value.
     *
     * <p>⚠️ The source and the owner come from the draft it was first saved with, never from a later
     * one — the same rule a persistent store keeps, so the two cannot disagree about what an update may
     * change.</p>
     */
    private static final class MemorySavedQuery implements SavedQuery {

        private final String          identifier;
        private final SavedQueryDraft draft;
        private final LocalDateTime   createdAt;
        private final LocalDateTime   updatedAt;

        private MemorySavedQuery(String identifier, SavedQueryDraft draft,
                                 LocalDateTime createdAt, LocalDateTime updatedAt) {
            this.identifier = identifier;
            this.draft      = draft;
            this.createdAt  = createdAt;
            this.updatedAt  = updatedAt;
        }

        private SavedQueryDraft rewrittenWith(SavedQueryDraft other) {
            return new SavedQueryDraft(
                    draft.source(), draft.owner(),
                    other.name(), other.description(), other.body(),
                    other.author(), other.shared(), other.sortOrder());
        }

        @Override
        public String getIdentifier() {
            return identifier;
        }

        @Override
        public String getSource() {
            return draft.source();
        }

        @Override
        public QueryOwner getOwner() {
            return draft.owner();
        }

        @Override
        public String getName() {
            return draft.name();
        }

        @Override
        public String getDescription() {
            return draft.description();
        }

        @Override
        public String getBody() {
            return draft.body();
        }

        @Override
        public String getAuthor() {
            return draft.author();
        }

        @Override
        public boolean isShared() {
            return draft.shared();
        }

        @Override
        public int getSortOrder() {
            return draft.sortOrder();
        }

        @Override
        public LocalDateTime getCreatedAt() {
            return createdAt;
        }

        @Override
        public LocalDateTime getUpdatedAt() {
            return updatedAt;
        }

        @Override
        public String toString() {
            return "%s '%s' on %s".formatted(identifier, draft.name(), draft.source());
        }
    }
}
