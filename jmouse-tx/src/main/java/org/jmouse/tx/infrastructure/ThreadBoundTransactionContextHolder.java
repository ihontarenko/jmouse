package org.jmouse.tx.infrastructure;

public class ThreadBoundTransactionContextHolder implements TransactionContextHolder {

    private static final ThreadLocal<TransactionContext> CONTEXT = new ThreadLocal<>();

    @Override
    public TransactionContext getCurrent() {
        return CONTEXT.get();
    }

    @Override
    public void bind(TransactionContext context) {
        CONTEXT.set(context);
    }

    @Override
    public void clear() {
        CONTEXT.remove();
    }
}
