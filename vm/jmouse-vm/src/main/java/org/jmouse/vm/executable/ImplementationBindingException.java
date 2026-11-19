package org.jmouse.vm.executable;

import java.util.Objects;

/** An explicit override must name the implementation it expects to replace. */
public final class ImplementationBindingException extends IllegalArgumentException {

    private final ImplementationIdentity expected;
    private final ImplementationIdentity actual;

    public ImplementationBindingException(
            ImplementationIdentity expected, ImplementationIdentity actual) {
        super("Implementation replacement expected " + expected + " but found " + actual);
        this.expected = Objects.requireNonNull(expected, "expected");
        this.actual = Objects.requireNonNull(actual, "actual");
    }

    public ImplementationIdentity expected() {
        return expected;
    }

    public ImplementationIdentity actual() {
        return actual;
    }
}
