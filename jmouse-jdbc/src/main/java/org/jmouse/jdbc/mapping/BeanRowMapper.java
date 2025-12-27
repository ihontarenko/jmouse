package org.jmouse.jdbc.mapping;

import org.jmouse.core.Contract;
import org.jmouse.core.bind.Bind;
import org.jmouse.core.bind.Binder;
import org.jmouse.jdbc.mapping.bind.JdbcAccessorWrapper;

import java.sql.ResultSet;

public final class BeanRowMapper<T> implements RowMapper<T> {

    private final Class<T> type;

    public BeanRowMapper(Class<T> type) {
        this.type = Contract.nonNull(type, "type");
    }

    public static <T> BeanRowMapper<T> of(Class<T> type) {
        return new BeanRowMapper<>(type);
    }

    @Override
    public T map(ResultSet resultSet) {
        return Bind.with(new Binder(JdbcAccessorWrapper.WRAPPER.wrap(resultSet))).get(type);
    }

}
