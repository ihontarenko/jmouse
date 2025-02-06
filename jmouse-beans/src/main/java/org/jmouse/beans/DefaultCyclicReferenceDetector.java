package org.jmouse.beans;

import org.jmouse.util.Visitor;

import java.util.function.Supplier;

/**
 * Default implementation of the {@link CyclicReferenceDetector} interface for detecting
 * cyclic dependencies among beans or objects of type {@code T}. It uses a thread-local
 * {@link Visitor} to keep track of visited identifiers during dependency resolution,
 * allowing the detection of cycles in the object graph.
 *
 * @param <T> the type of identifiers being tracked for cyclic references
 */
public class DefaultCyclicReferenceDetector<T> implements CyclicReferenceDetector<T> {

    /**
     * Thread-local stack of visited references, implemented using a {@link Visitor.Default}.
     * This ensures that cyclic reference detection is thread-safe and isolated per thread.
     */
    private final ThreadLocal<Visitor<T>> referenceStack = ThreadLocal.withInitial(Visitor.Default::new);

    /**
     * Detects a cyclic reference for the given identifier. If the identifier has been
     * encountered previously in the current detection chain, a cyclic dependency is detected.
     * In such a case, this method constructs a dependency chain representation and throws
     * a {@link RuntimeException} provided by the {@code exceptionSupplier}.
     *
     * @param identifier       the identifier to check for cyclic references
     * @param exceptionSupplier a supplier that provides a {@link RuntimeException} to be thrown
     *                          if a cyclic reference is detected
     * @throws RuntimeException if a cyclic reference is detected
     */
    @Override
    public void detect(Identifier<T> identifier, Supplier<? extends RuntimeException> exceptionSupplier) {
        Visitor<T> visitor = referenceStack.get();
        T        id    = identifier.getIdentifier();

        if (visitor.familiar(id)) {
            // Construct a dependency chain representation
            String dependencyChain = visitor.encounters().stream().map(Object::toString)
                    .reduce("%s -> %s"::formatted).map(chain -> "%s -> %s".formatted(chain, id))
                    .orElse(id.toString());

            // Clear the visitor to avoid lingering references
            visitor.erase();

            // Throw the exception with a cause
            throw (RuntimeException) exceptionSupplier.get().initCause(
                    new IllegalStateException("Cyclic reference detected: %s".formatted(dependencyChain)));
        }

        // Push the current identifier onto the visitor
        visitor.visit(id);
    }

    /**
     * Removes the provided identifier from the current tracking chain. This method is typically
     * called after the processing of a dependency is complete to allow for proper unwinding
     * of the visited elements stack.
     *
     * @param identifier the identifier to remove from the current chain
     */
    @Override
    public void remove(Identifier<T> identifier) {
        Visitor<T> visitor = referenceStack.get();
        visitor.forget(identifier.getIdentifier());
    }
}
