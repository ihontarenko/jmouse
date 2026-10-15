package org.jmouse.telegram.jpa;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.TypedQuery;
import org.jmouse.telegram.journal.JournalEntry;
import org.jmouse.telegram.journal.JournalPage;
import org.jmouse.telegram.journal.JournalQuery;
import org.jmouse.telegram.journal.TelegramJournal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * The journal in the database, in {@code telegram_journal}.
 *
 * <h2>⚠️ Its own transaction, every time — and that is the point</h2>
 *
 * <p>Unlike the other stores here, this one does not join the caller's transaction. It takes an
 * {@link EntityManagerFactory}, opens its own entity manager and commits its own resource-local
 * transaction per write. Two reasons, both about what a journal is for:
 *
 * <ol>
 *   <li>A send is often made inside somebody's save. If that save rolls back, the message has
 *       <em>still gone</em> — Telegram cannot un-send it — and a journal that rolled back with it would
 *       deny a message that a person is holding in their hand.</li>
 *   <li>A send is just as often made where there is no transaction at all: a retry worker, a polling
 *       handler. A store that needed one would fail exactly there.</li>
 * </ol>
 *
 * <p>⚠️ That needs a {@code RESOURCE_LOCAL} persistence unit — the default for Spring Boot's
 * {@code LocalContainerEntityManagerFactoryBean}. Under JTA, replace this bean.
 *
 * <p>⚠️ A write that fails is logged and swallowed: by the time it is written, the call it describes has
 * already happened (see {@link TelegramJournal}).
 */
public final class JpaTelegramJournal implements TelegramJournal {

    private static final Logger LOGGER = LoggerFactory.getLogger(JpaTelegramJournal.class);

    private final EntityManagerFactory entityManagerFactory;

    public JpaTelegramJournal(EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = Objects.requireNonNull(entityManagerFactory, "entity manager factory");
    }

    @Override
    public void record(JournalEntry entry) {
        try {
            inTransaction(entityManager -> {
                entityManager.persist(TelegramJournalRecord.of(entry));
                return null;
            });
        } catch (RuntimeException exception) {
            LOGGER.warn("a Telegram journal entry could not be written", exception);
        }
    }

    @Override
    public JournalPage list(JournalQuery query) {
        Filter filter = Filter.of(query);

        return inTransaction(entityManager -> {
            TypedQuery<TelegramJournalRecord> rows = entityManager.createQuery(
                    "SELECT record FROM TelegramJournalRecord record" + filter.where()
                            + " ORDER BY record.recordedAt DESC", TelegramJournalRecord.class);
            TypedQuery<Long> count = entityManager.createQuery(
                    "SELECT COUNT(record) FROM TelegramJournalRecord record" + filter.where(), Long.class);

            filter.bind(rows);
            filter.bind(count);

            List<JournalEntry> entries = rows.setFirstResult(query.offset())
                                             .setMaxResults(query.limit())
                                             .getResultList()
                                             .stream()
                                             .map(TelegramJournalRecord::toEntry)
                                             .toList();

            return new JournalPage(entries, count.getSingleResult());
        });
    }

    @Override
    public long prune(Instant before) {
        Objects.requireNonNull(before, "before");

        return inTransaction(entityManager -> (long) entityManager
                .createQuery("DELETE FROM TelegramJournalRecord record WHERE record.recordedAt < :before")
                .setParameter("before", before)
                .executeUpdate());
    }

    private <T> T inTransaction(Function<EntityManager, T> work) {
        EntityManager     entityManager = entityManagerFactory.createEntityManager();
        EntityTransaction transaction   = entityManager.getTransaction();

        try {
            transaction.begin();
            T result = work.apply(entityManager);
            transaction.commit();
            return result;
        } catch (RuntimeException exception) {
            if (transaction.isActive()) {
                transaction.rollback();
            }

            throw exception;
        } finally {
            entityManager.close();
        }
    }

    /** The WHERE clause a query's filters add, and the parameters it binds — kept together so they agree. */
    private record Filter(String where, Map<String, Object> parameters) {

        static Filter of(JournalQuery query) {
            List<String>        conditions = new ArrayList<>();
            Map<String, Object> parameters = new LinkedHashMap<>();

            if (query.identityName() != null) {
                conditions.add("record.identityName = :identityName");
                parameters.put("identityName", query.identityName());
            }

            if (query.chat() != null) {
                conditions.add("record.chat = :chat");
                parameters.put("chat", query.chat());
            }

            if (query.outcome() != null) {
                conditions.add("record.outcome = :outcome");
                parameters.put("outcome", query.outcome());
            }

            if (query.since() != null) {
                conditions.add("record.recordedAt >= :since");
                parameters.put("since", query.since());
            }

            String where = conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions);

            return new Filter(where, parameters);
        }

        void bind(TypedQuery<?> query) {
            parameters.forEach(query::setParameter);
        }
    }
}
