package org.jmouse.parallax.object;

import java.util.Optional;

/**
 * Where a circuit's {@code MODEL} elements, a component's logic family and the function libraries its equations call
 * find their jMX source. The library ships none of them: they are the product's catalogue, which resolves a reference
 * alone ({@code lm358}) to the current document and one with a revision ({@code my-pwm@3}) to that revision, so that
 * editing a model never silently changes a circuit that used an earlier one.
 */
@FunctionalInterface
public interface ComponentLibrary {

    /** The jMX source the reference names, or empty when this library does not hold it. */
    Optional<String> source(String reference);

    /** A library that holds nothing: for objects that reference no component. */
    static ComponentLibrary empty() {
        return reference -> Optional.empty();
    }
}
