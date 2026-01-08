package org.jmouse.core.context;

import java.util.Map;

public interface ExecutionContext {

    <T> T get(ContextKey<T> key);

    <T> ExecutionContext with(ContextKey<T> key, T value);

    Map<ContextKey<?>, Object> entries();

    ExecutionSnapshot snapshot();

}