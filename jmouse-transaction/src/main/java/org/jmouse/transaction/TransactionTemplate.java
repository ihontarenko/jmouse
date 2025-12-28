package org.jmouse.transaction;

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
public class TransactionTemplate implements TransactionCallback {

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
    public <T> T inTransaction(TransactionDefinition definition, Work<T> work){
        TransactionStatus status = coordinator.begin(definition);

        try {
            T result = work.run();
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

}
