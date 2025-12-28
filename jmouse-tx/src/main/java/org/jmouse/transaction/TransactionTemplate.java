package org.jmouse.transaction;

import java.util.concurrent.Callable;

public class TransactionTemplate {

    private final TransactionManager coordinator;

    public TransactionTemplate(TransactionManager coordinator) {
        this.coordinator = coordinator;
    }

    public void execute(TransactionDefinition definition, Worker work) {
        execute(definition, () -> {
            work.run();
            return null;
        });
    }

    public <T> T execute(TransactionDefinition definition, Callable<T> work) {
        TransactionStatus status = coordinator.begin(definition);
        try {
            T result = work.call();
            coordinator.commit(status);
            return result;
        } catch (Throwable throwable) {
            try {
                coordinator.rollback(status);
            } catch (Throwable ignored) {}
            throw (throwable instanceof RuntimeException exception) ? exception : new RuntimeException(throwable);
        }
    }

    @FunctionalInterface
    public interface Worker {
        <T> T run() throws Exception;
    }

}
