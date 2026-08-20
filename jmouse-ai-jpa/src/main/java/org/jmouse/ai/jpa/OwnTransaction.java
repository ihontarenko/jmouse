package org.jmouse.ai.jpa;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A unit of work that does not join whatever transaction it was called from.
 *
 * <p>⚠️ <strong>This is not a convenience, it is the point of three classes in this package.</strong>
 * Counting a call, and spending a confirmation token, are bookkeeping <em>about</em> work rather than
 * part of it, and joining the caller's transaction gets both wrong in the same way: a counter rolled
 * back with a failed call under-reports exactly the failures that matter most, and a token un-spent by
 * a rollback is a token that can be spent again.
 *
 * <p>Its own {@link EntityManager} rather than a propagation setting, because this library has no
 * transaction manager and should not acquire one — Jakarta Persistence alone, so transaction
 * demarcation stays entirely with whoever calls.
 *
 * <p>⚠️ Which means a product using container-managed transactions gets a second connection from the
 * pool for the duration. That is the cost, it is real, and it is smaller than the alternative.
 */
/*
 * ⚠️ Public because `JpaClientNameRegistry` lives in the module that owns `ClientNameRegistry`
 * rather than in this one — the interface is a Spring module’s and the schema is this one’s, and
 * neither should take a hard dependency on the other just to place one class. Everything above
 * still applies: this is not a convenience.
 */
public final class OwnTransaction {

    private OwnTransaction() {
    }

    public static void run(EntityManagerFactory factory, Consumer<EntityManager> work) {
        call(factory, entityManager -> {
            work.accept(entityManager);
            return null;
        });
    }

    public static <T> T call(EntityManagerFactory factory, Function<EntityManager, T> work) {
        try (EntityManager entityManager = factory.createEntityManager()) {
            EntityTransaction transaction = entityManager.getTransaction();
            transaction.begin();

            try {
                T answer = work.apply(entityManager);
                transaction.commit();

                return answer;

            } catch (RuntimeException failure) {
                if (transaction.isActive()) {
                    transaction.rollback();
                }

                throw failure;
            }
        }
    }
}
