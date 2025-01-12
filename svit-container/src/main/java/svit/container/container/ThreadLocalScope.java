package svit.container.container;

import svit.container.Scope;

/**
 * Enum representing a thread-local {@link Scope}.
 * <p>
 * This scope is used to manage beans in a thread-local context, ensuring that
 * each thread has its own independent set of beans.
 */
public enum ThreadLocalScope implements Scope {

    /**
     * The singleton instance of the thread-local scope.
     */
    THREAD_LOCAL_SCOPE;

    /**
     * Returns the unique identifier for this scope.
     * <p>
     * The ID is a constant value (1000) that uniquely identifies the thread-local scope.
     *
     * @return the unique ID of the thread-local scope.
     */
    @Override
    public int id() {
        return 1000;
    }

}
