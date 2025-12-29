package org.jmouse.transaction;

public class TransactionOperation {

    private final TransactionCallback   callback;
    private final TransactionDefinition defaults;

    public TransactionOperation(TransactionCallback callback, TransactionDefinition defaults) {
        this.callback = callback;
        this.defaults = defaults;
    }

    public <T> T required(TransactionCallback.Work<T> work) {
        return callback.inTransaction(defaults, work);
    }

    public <T> T requiresNew(TransactionCallback.Work<T> work) {
        TransactionDefinition definition = DefaultTransactionDefinition.builder()
                .propagation(TransactionPropagation.REQUIRES_NEW)
                .isolation(defaults.getIsolation())
                .timeoutSeconds(defaults.getTimeoutSeconds())
                .readOnly(defaults.isReadOnly())
                .name(defaults.getName())
                .build();
        return callback.inTransaction(definition, work);
    }

}
