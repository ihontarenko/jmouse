package org.jmouse.money.jpa;

import jakarta.persistence.EntityManager;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 🗄️ Rows in and rows out. Nothing else.
 *
 * <p>⚠️ <strong>No transaction demarcation, and that is the library's side of a bargain.</strong> A
 * library that opened its own transactions would either fight the product's or quietly commit half of
 * something the product was still deciding about. Every method here assumes it is inside one the
 * caller opened — which is the same contract {@code ManagedFiles} and {@code FileBindings} keep.</p>
 *
 * <p>⚠️ <strong>It knows nothing about what a rate means.</strong> Whether a manual row may be
 * overwritten, whether a pivot may change, what to do about a feed that returned a currency nobody
 * recognises — all of that is policy, and policy lives in the service above. This class would answer
 * any of those questions the same way whatever the answer should be, which is why it is not asked.</p>
 */
public final class ExchangeRateRegistry {

    private final EntityManager entityManager;

    public ExchangeRateRegistry(EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "A registry needs an entity manager");
    }

    /**
     * 🔎 One currency's row, if the table holds it against this pivot.
     *
     * <p>⚠️ The pivot is part of the question. A row quoted against a different pivot is not a stale
     * answer to this one, it is an answer to another question entirely, and returning it would convert
     * by a number that means something else.</p>
     */
    public Optional<ExchangeRate> find(String currency, String pivot) {
        return Optional.ofNullable(entityManager.find(ExchangeRate.class, currency))
                .filter(rate -> rate.getPivot().equals(pivot));
    }

    /** 🔎 One currency's row whatever it is quoted against — for a screen, and for detecting a changed feed. */
    public Optional<ExchangeRate> find(String currency) {
        return Optional.ofNullable(entityManager.find(ExchangeRate.class, currency));
    }

    /** 📋 Every rate held against this pivot, alphabetically. */
    public List<ExchangeRate> all(String pivot) {
        return entityManager
                .createQuery("SELECT rate FROM ExchangeRate rate WHERE rate.pivot = :pivot ORDER BY rate.currency",
                             ExchangeRate.class)
                .setParameter("pivot", pivot)
                .getResultList();
    }

    /** 📋 Every rate in the table, alphabetically — including any left behind by a previous pivot. */
    public List<ExchangeRate> all() {
        return entityManager
                .createQuery("SELECT rate FROM ExchangeRate rate ORDER BY rate.currency", ExchangeRate.class)
                .getResultList();
    }

    /**
     * 🕒 The newest write against this pivot.
     *
     * <p>Empty where nothing has ever been stored — which is a fresh installation, not a fault, and the
     * two look identical to anything that only counts rows.</p>
     */
    public Optional<Instant> lastUpdated(String pivot) {
        return Optional.ofNullable(entityManager
                .createQuery("SELECT MAX(rate.updatedAt) FROM ExchangeRate rate WHERE rate.pivot = :pivot",
                             Instant.class)
                .setParameter("pivot", pivot)
                .getSingleResult());
    }

    /**
     * 🔎 Every pivot the table currently holds rows against.
     *
     * <p>⚠️ <strong>More than one is a broken table, and this is how anybody finds out.</strong> It
     * happens when a feed is swapped without clearing what the previous one wrote, and nothing else in
     * the system would notice: each row is individually plausible, and only the set of them is wrong.</p>
     */
    public Set<String> pivots() {
        return entityManager
                .createQuery("SELECT DISTINCT rate.pivot FROM ExchangeRate rate", String.class)
                .getResultList()
                .stream()
                .collect(Collectors.toUnmodifiableSet());
    }

    /** 💾 Writes a rate, inserting or updating as the row demands. */
    public ExchangeRate save(ExchangeRate rate) {
        return entityManager.merge(rate);
    }

    /** 🗑️ Removes one currency's row — how a pivot change clears what the old feed left behind. */
    public void remove(String currency) {
        find(currency).ifPresent(entityManager::remove);
    }
}
