package org.jmouse.jdbc.statement;

@FunctionalInterface
public interface BinderFactory<T> {
    PreparedStatementBinder binderFor(T item);
}
