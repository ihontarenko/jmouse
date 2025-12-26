package org.jmouse.jdbc.mapping;

import org.jmouse.core.Contract;
import org.jmouse.core.bind.Bind;
import org.jmouse.core.bind.Binder;
import org.jmouse.jdbc.mapping.bind.JdbcAccessorWrapper;

import java.sql.ResultSet;

public final class BeanRowMapper<T> implements RowMapper<T> {

    private final Class<T>     type;
    private final NameStrategy strategy;

    public BeanRowMapper(Class<T> type, NameStrategy strategy) {
        this.type = Contract.nonNull(type, "type");
        this.strategy = Contract.nonNull(strategy, "strategy");
    }

    public static <T> BeanRowMapper<T> of(Class<T> type) {
        return new BeanRowMapper<>(type, NameStrategy.toCamel());
    }

    public static <T> BeanRowMapper<T> of(Class<T> type, NameStrategy strategy) {
        return new BeanRowMapper<>(type, strategy);
    }

    @Override
    public T map(ResultSet resultSet) {
        Binder binder = new Binder(JdbcAccessorWrapper.WRAPPER.wrap(resultSet));
        return Bind.with(binder).get(type);
    }

}
