package org.jmouse.core.mapping.bindings;

@FunctionalInterface
public interface SourceValueProvider<S> {
    Object provide(S source);
}