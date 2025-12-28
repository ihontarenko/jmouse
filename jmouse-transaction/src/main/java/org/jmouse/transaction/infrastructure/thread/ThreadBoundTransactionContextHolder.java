package org.jmouse.transaction.infrastructure.thread;

import org.jmouse.transaction.infrastructure.TransactionContext;
import org.jmouse.transaction.infrastructure.TransactionContextHolder;

import java.util.HashMap;
import java.util.Map;

/**
 * Thread-bound implementation of {@link TransactionContextHolder}.
 * <p>
 * This holder binds {@link TransactionContext transaction contexts} and
 * arbitrary transactional resources to the current thread using {@link ThreadLocal}.
 *
 * <h3>Key characteristics</h3>
 * <ul>
 *     <li>Supports <b>nested transactions</b> via a stack (linked nodes)</li>
 *     <li>Ensures strict one-resource-per-key binding</li>
 *     <li>Designed for synchronous, thread-confined execution models</li>
 * </ul>
 *
 * <h3>Context stack model</h3>
 * <pre>{@code
 * bindContext(TX1)
 *   └── bindContext(TX2)
 *         └── unbindContext() → TX2
 *   └── unbindContext() → TX1
 * }</pre>
 *
 * <p>
 * ⚠️ Contexts and resources <b>must</b> be unbound in the same thread
 * in which they were bound.
 *
 * @author jMouse
 */
public final class ThreadBoundTransactionContextHolder
        implements TransactionContextHolder {

    /**
     * Thread-local stack of transaction contexts.
     * <p>
     * The head node represents the currently active transaction.
     */
    private static final ThreadLocal<Node> CONTEXT_STACK = new ThreadLocal<>();

    /**
     * Thread-local storage for transaction-scoped resources
     * (e.g. JDBC connections, sessions).
     */
    private static final ThreadLocal<Map<Class<?>, Object>> RESOURCE_HOLDER =
            ThreadLocal.withInitial(HashMap::new);

    /**
     * Returns the currently active {@link TransactionContext} for this thread.
     *
     * @return current context or {@code null} if none is bound
     */
    @Override
    public TransactionContext getContext() {
        Node head = CONTEXT_STACK.get();
        return head != null ? head.context() : null;
    }

    /**
     * Binds a new {@link TransactionContext} to the current thread.
     * <p>
     * If a context is already present, the new one becomes the head
     * of the context stack (nested transaction).
     *
     * @param context transaction context to bind
     */
    @Override
    public void bindContext(TransactionContext context) {
        Node head = CONTEXT_STACK.get();
        CONTEXT_STACK.set(new Node(context, head));
    }

    /**
     * Unbinds the current {@link TransactionContext} from the thread.
     * <p>
     * If the context was nested, the parent context becomes active again.
     *
     * @return the unbound context, or {@code null} if none was bound
     */
    @Override
    public TransactionContext unbindContext() {
        Node head = CONTEXT_STACK.get();

        if (head == null) {
            return null;
        }

        CONTEXT_STACK.set(head.parent());

        return head.context();
    }

    /**
     * Binds a transactional resource to the current thread.
     * <p>
     * Only one resource per key type is allowed.
     *
     * @param key      resource key (usually a holder or handle class)
     * @param resource resource instance to bind
     * @param <T>      resource type
     * @throws IllegalStateException if a resource is already bound for the key
     */
    @Override
    public <T> void bindResource(Class<T> key, T resource) {
        Map<Class<?>, Object> resources = RESOURCE_HOLDER.get();
        Object                previous  = resources.put(key, resource);

        if (previous != null) {
            resources.put(key, previous);
            throw new IllegalStateException(
                    "Resource already bound for key: " + key.getName()
            );
        }
    }

    /**
     * Returns a thread-bound resource by its key.
     *
     * @param key resource key
     * @param <T> resource type
     * @return bound resource or {@code null} if not present
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> T getResource(Class<T> key) {
        return (T) RESOURCE_HOLDER.get().get(key);
    }

    /**
     * Unbinds and returns a resource associated with the given key.
     *
     * @param key resource key
     * @param <T> resource type
     * @return previously bound resource or {@code null}
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> T unbindResource(Class<T> key) {
        return (T) RESOURCE_HOLDER.get().remove(key);
    }

    /**
     * Checks whether a resource is bound for the given key.
     *
     * @param key resource key
     * @return {@code true} if a resource is bound, {@code false} otherwise
     */
    @Override
    public boolean hasResource(Class<?> key) {
        return RESOURCE_HOLDER.get().containsKey(key);
    }


    /**
     * Creates a snapshot of the current thread-bound resources and clears them from this thread.
     * <p>
     * This method is intended for execution models where you need to:
     * <ul>
     *     <li>capture transaction-scoped resources (e.g. JDBC connection holders)</li>
     *     <li>temporarily detach them from the current thread</li>
     *     <li>optionally resume them later (possibly on another thread)</li>
     * </ul>
     *
     * <h3>Example</h3>
     * <pre>{@code
     * Map<Class<?>, Object> snapshot = holder.createSnapshot();
     * try {
     *     // run code without any bound resources in this thread
     * } finally {
     *     holder.applySnapshot(snapshot);
     * }
     * }</pre>
     *
     * @return immutable empty map if there are no resources; otherwise a mutable snapshot map
     */
    public Map<Class<?>, Object> createSnapshot() {
        Map<Class<?>, Object> current = RESOURCE_HOLDER.get();

        if (current.isEmpty()) {
            return Map.of();
        }

        Map<Class<?>, Object> snapshot = new HashMap<>(current);
        current.clear();

        return snapshot;
    }

    /**
     * Applies a previously captured resource snapshot to the current thread.
     * <p>
     * The operation is <b>fail-fast</b> when the current thread already has resources bound,
     * preventing accidental resource interleaving.
     *
     * <p>
     * ⚠️ This method does not validate the snapshot contents or ownership. It assumes the snapshot
     * was created by {@link #createSnapshot()} and that the caller manages lifecycle correctly.
     *
     * @param snapshot snapshot created by {@link #createSnapshot()}
     * @throws IllegalStateException if the current thread already has bound resources
     */
    @Override
    public void applySnapshot(Map<Class<?>, Object> snapshot) {
        if (snapshot == null || snapshot.isEmpty()) {
            return;
        }

        Map<Class<?>, Object> current = RESOURCE_HOLDER.get();

        if (!current.isEmpty()) {
            throw new IllegalStateException(
                    "Cannot apply snapshot: current state is not empty"
            );
        }

        current.putAll(snapshot);
    }

    /**
     * Clears all thread-bound transaction contexts and resources.
     * <p>
     * Intended to be called at the end of request or execution boundary.
     */
    @Override
    public void clear() {
        CONTEXT_STACK.remove();
        RESOURCE_HOLDER.remove();
    }

    /**
     * Internal stack node representing a single transaction context
     * and a reference to its parent (outer) context.
     */
    private record Node(TransactionContext context, Node parent) { }

}
