package org.jmouse.jdbc.statement;

import java.util.function.Function;

import static org.jmouse.core.Contract.nonNull;

@FunctionalInterface
public interface BinderFactory<T> {

    StatementBinder binderFor(T item);

    default BinderFactory<T> andThen(Function<StatementBinder, StatementBinder> wrapper) {
        return item -> nonNull(wrapper, "wrapper").apply(binderFor(item));
    }

    default BinderFactory<T> compose(Function<T, StatementBinder> prefix) {
        return item -> Binders.composeBinders(nonNull(prefix, "prefix").apply(item), binderFor(item));
    }

    static <T> BinderFactory<T> of(Function<T, StatementBinder> function) {
        return nonNull(function, "function")::apply;
    }

}
