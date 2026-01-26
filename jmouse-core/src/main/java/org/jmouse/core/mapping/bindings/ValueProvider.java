package org.jmouse.core.mapping.bindings;

@FunctionalInterface
public interface ValueProvider<S> {
    Object provide(S source);
}