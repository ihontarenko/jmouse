package svit.container;

import java.util.Stack;
import java.util.function.Supplier;

/**
 * Default implementation of {@link CyclicReferenceDetector} to detect cyclic references in a dependency graph.
 * <p>
 * This implementation uses a {@link ThreadLocal} {@link Stack} to track the current chain of references for each thread,
 * ensuring thread-safe detection of cycles.
 *
 * @param <T> the type of identifier used to detect cyclic references.
 */
public class DefaultCyclicReferenceDetector<T> implements CyclicReferenceDetector<T> {

    /**
     * Thread-local stack to maintain the chain of references for detecting cycles.
     */
    private final ThreadLocal<Stack<T>> referenceStack = ThreadLocal.withInitial(Stack::new);

    /**
     * Detects a cyclic reference by checking if the provided identifier is already in the current chain.
     * <p>
     * If a cyclic reference is detected, the stack is cleared, and a runtime exception is thrown
     * with details about the detected cycle.
     *
     * @param identifier        the identifier representing the current reference being processed.
     * @param exceptionSupplier a supplier that provides a custom exception to throw in case of a cycle.
     * @throws Throwable the exception provided by the supplier in case of a cycle.
     */
    @Override
    public void detect(Identifier<T> identifier, Supplier<? extends RuntimeException> exceptionSupplier) {
        Stack<T> stack = referenceStack.get();
        T        id    = identifier.getIdentifier();

        if (stack.contains(id)) {
            // Construct a dependency chain representation
            String dependencyChain = stack.stream().map(Object::toString)
                    .reduce("%s -> %s"::formatted).map(chain -> "%s -> %s".formatted(chain, id))
                    .orElse(id.toString());

            // Clear the stack to avoid lingering references
            stack.clear();

            // Throw the exception with a cause
            throw (RuntimeException) exceptionSupplier.get().initCause(
                    new IllegalStateException("Cyclic reference detected: %s".formatted(dependencyChain)));
        }

        // Push the current identifier onto the stack
        stack.push(id);
    }

    /**
     * Removes the provided identifier from the current chain, typically called after processing is complete.
     *
     * @param identifier the identifier to remove from the current chain.
     */
    @Override
    public void remove(Identifier<T> identifier) {
        Stack<T> stack = referenceStack.get();
        stack.remove(identifier.getIdentifier());
    }
}
