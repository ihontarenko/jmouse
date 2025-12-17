package org.jmouse.tx.infrastructure.thread;

import org.jmouse.tx.infrastructure.TransactionContext;
import org.jmouse.tx.infrastructure.TransactionContextHolder;

import java.util.HashMap;
import java.util.Map;

public final class ThreadBoundTransactionContextHolder
        implements TransactionContextHolder {

    private static final ThreadLocal<TransactionContext> CONTEXT_HOLDER =
            new ThreadLocal<>();

    private static final ThreadLocal<Map<Class<?>, Object>> RESOURCE_HOLDER =
            ThreadLocal.withInitial(HashMap::new);

    @Override
    public TransactionContext getContext() {
        return CONTEXT_HOLDER.get();
    }

    @Override
    public void bindContext(TransactionContext context) {
        CONTEXT_HOLDER.set(context);
    }

    @Override
    public TransactionContext unbindContext() {
        TransactionContext ctx = CONTEXT_HOLDER.get();
        CONTEXT_HOLDER.remove();
        return ctx;
    }

    @Override
    public <T> void bindResource(Class<T> key, T resource) {
        RESOURCE_HOLDER.get().put(key, resource);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getResource(Class<T> key) {
        return (T) RESOURCE_HOLDER.get().get(key);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T unbindResource(Class<T> key) {
        return (T) RESOURCE_HOLDER.get().remove(key);
    }

    @Override
    public boolean hasResource(Class<?> key) {
        return RESOURCE_HOLDER.get().containsKey(key);
    }

    @Override
    public void clear() {
        CONTEXT_HOLDER.remove();
        RESOURCE_HOLDER.remove();
    }
}
