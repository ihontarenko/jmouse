package org.jmouse.jdbc.bulk;

import org.jmouse.jdbc.statement.PreparedStatementBinder;

@FunctionalInterface
public interface BinderFactory<T> {
    PreparedStatementBinder binderFor(T item);
}
