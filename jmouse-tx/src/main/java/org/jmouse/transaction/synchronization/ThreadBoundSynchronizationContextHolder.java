package org.jmouse.transaction.synchronization;

import org.jmouse.core.Contract;

/**
 * Thread-bound implementation of {@link SynchronizationContextHolder}.
 * <p>
 * This holder manages {@link SynchronizationContext} instances bound to the
 * current thread and supports <b>nested synchronization scopes</b>
 * using a stack-based model.
 *
 * <h3>Typical use cases</h3>
 * <ul>
 *     <li>Transaction lifecycle callbacks (beforeCommit, afterCompletion)</li>
 *     <li>Resource synchronization coordination</li>
 *     <li>Framework-level transaction orchestration</li>
 * </ul>
 *
 * <h3>Stack behavior</h3>
 * <pre>{@code
 * bind(SYNC1)
 *   └── bind(SYNC2)
 *         └── unbind() → SYNC2
 *   └── unbind() → SYNC1
 * }</pre>
 *
 * <p>
 * ⚠️ All bind / unbind operations must occur within the same thread.
 *
 * @author jMouse
 */
final public class ThreadBoundSynchronizationContextHolder
        implements SynchronizationContextHolder {

    /**
     * Thread-local stack of synchronization contexts.
     * <p>
     * The head node represents the currently active synchronization context.
     */
    private static final ThreadLocal<Node> STACK = new ThreadLocal<>();

    /**
     * Returns the currently active {@link SynchronizationContext} for this thread.
     *
     * @return current synchronization context or {@code null} if none is bound
     */
    @Override
    public SynchronizationContext getCurrent() {
        Node head = STACK.get();
        return head != null ? head.context() : null;
    }

    /**
     * Binds a {@link SynchronizationContext} to the current thread.
     * <p>
     * If another context is already bound, the new one becomes nested
     * and takes precedence.
     *
     * @param context synchronization context to bind (must not be {@code null})
     */
    @Override
    public void bind(SynchronizationContext context) {
        Contract.nonNull(context, "context");
        Node head = STACK.get();
        STACK.set(new Node(context, head));
    }

    /**
     * Unbinds the current {@link SynchronizationContext}.
     * <p>
     * If the context was nested, the parent context becomes active again.
     *
     * @return the unbound synchronization context, or {@code null} if none was bound
     */
    @Override
    public SynchronizationContext unbind() {
        Node head = STACK.get();

        if (head == null) {
            return null;
        }

        STACK.set(head.parent());
        return head.context();
    }

    /**
     * Clears all synchronization contexts bound to the current thread.
     * <p>
     * Intended to be called at the end of a transaction or execution boundary.
     */
    @Override
    public void clear() {
        STACK.remove();
    }

    /**
     * Internal stack node representing a single synchronization context
     * and a reference to its parent (outer) context.
     */
    private record Node(
            SynchronizationContext context,
            Node parent
    ) { }

}
