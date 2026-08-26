package org.jmouse.mapper.binding;

@FunctionalInterface
public interface ValueProvider<S> {
    Object provide(S source);
}