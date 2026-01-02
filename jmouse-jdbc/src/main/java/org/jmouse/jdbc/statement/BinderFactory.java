package org.jmouse.jdbc.statement;

import java.util.function.Function;

import static org.jmouse.core.Contract.nonNull;

@FunctionalInterface
public interface BinderFactory<T> {

    PreparedStatementBinder binderFor(T item);

    default BinderFactory<T> andThen(Function<PreparedStatementBinder, PreparedStatementBinder> wrapper) {
        return item -> nonNull(wrapper, "wrapper").apply(binderFor(item));
    }

    default BinderFactory<T> compose(Function<T, PreparedStatementBinder> prefix) {
        return item -> Binders.composeBinders(nonNull(prefix, "prefix").apply(item), binderFor(item));
    }

    static <T> BinderFactory<T> of(Function<T, PreparedStatementBinder> function) {
        return nonNull(function, "function")::apply;
    }

}
