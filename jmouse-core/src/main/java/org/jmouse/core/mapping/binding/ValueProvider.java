package org.jmouse.core.mapping.binding;

@FunctionalInterface
public interface ValueProvider<S> {
    Object provide(S source);
}