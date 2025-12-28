package org.jmouse.transaction;

import java.util.concurrent.Callable;

/**
 * {@code TransactionTemplate} is a programmatic transaction boundary helper.
 * <p>
 * It follows the <b>template method</b> pattern and encapsulates the
 * boilerplate logic of:
 * <ul>
 *     <li>starting a transaction</li>
 *     <li>executing user logic</li>
 *     <li>committing on success</li>
 *     <li>rolling back on failure</li>
 * </ul>
 *
 * <p>
 * This class is conceptually similar to Spring's {@code TransactionTemplate},
 * but aligned with jMouse TX abstractions.
 *
 * <h3>Typical usage</h3>
 * <pre>{@code
 * TransactionTemplate tx = new TransactionTemplate(transactionManager);
 *
 * tx.execute(definition, () -> {
 *     repository.save(entity);
 * });
 *
 * Integer count = tx.execute(definition, () -> repository.count());
 * }</pre>
 *
 * <p>
 * ⚠️ Checked exceptions thrown by user code are wrapped into {@link RuntimeException}.
 *
 * @author jMouse
 */
public class TransactionTemplate {

    /**
     * Underlying transaction manager coordinating begin/commit/rollback
     */
    private final TransactionManager coordinator;

    /**
     * Creates a new {@code TransactionTemplate}.
     *
     * @param coordinator transaction manager to delegate to
     */
    public TransactionTemplate(TransactionManager coordinator) {
        this.coordinator = coordinator;
    }

    /**
     * Executes transactional work without a return value.
     *
     * @param definition transaction definition (propagation, isolation, etc.)
     * @param work       unit of work to execute
     */
    public void execute(TransactionDefinition definition, Worker work) {
        execute(definition, () -> {
            work.run();
            return null;
        });
    }

    /**
     * Executes transactional work with a return value.
     * <p>
     * Transaction lifecycle:
     * <ol>
     *     <li>{@code begin}</li>
     *     <li>execute user code</li>
     *     <li>{@code commit} on success</li>
     *     <li>{@code rollback} on any failure</li>
     * </ol>
     *
     * @param definition transaction definition
     * @param work       unit of work to execute
     * @param <T>        return type
     * @return result of the work
     */
    public <T> T execute(TransactionDefinition definition, Callable<T> work) {
        TransactionStatus status = coordinator.begin(definition);

        try {
            T result = work.call();
            coordinator.commit(status);
            return result;
        } catch (Throwable throwable) {
            try {
                coordinator.rollback(status);
            } catch (Throwable ignored) {
                // rollback failures are deliberately suppressed
            }
            throw (throwable instanceof RuntimeException exception)
                    ? exception
                    : new RuntimeException(throwable);
        }
    }

    /**
     * Functional contract for transactional work without enforcing {@link Callable}.
     * <p>
     * Intended for concise lambda usage.
     */
    @FunctionalInterface
    public interface Worker {

        /**
         * Executes user logic within a transactional boundary.
         *
         * @param <T> return type (may be {@link Void})
         * @return execution result
         * @throws Exception any failure triggering rollback
         */
        <T> T run() throws Exception;
    }

}
