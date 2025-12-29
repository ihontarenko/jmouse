package org.jmouse.jdbc.mapping;

import org.jmouse.core.Contract;
import org.jmouse.core.bind.*;
import org.jmouse.core.bind.accessor.DummyObjectAccessor;
import org.jmouse.core.reflection.InferredType;
import org.jmouse.jdbc.mapping.bind.JdbcAccessorWrapper;

import java.sql.ResultSet;

public final class BeanRowMapper<T> implements RowMapper<T> {

    private final Class<T> type;
    private final Binder binder;

    public BeanRowMapper(Class<T> type) {
        this.type = Contract.nonNull(type, "type");
        this.binder = new Binder(new DummyObjectAccessor(null));
    }

    public static <T> BeanRowMapper<T> of(Class<T> type) {
        return new BeanRowMapper<>(type);
    }

    @Override
    public T map(ResultSet resultSet, int rowIndex) {
        ObjectAccessor accessor = JdbcAccessorWrapper.WRAPPER.wrap(resultSet);
        binder.setObjectAccessor(accessor);
        BindResult<T> result = binder.bind(null, toBindable(type));
        Contract.state(result.isPresent(), "Failed to map '" + type + "'.");
        return result.getValue();
    }

    private Bindable<T> toBindable(Class<T> type) {
        return Bindable.of(InferredType.forClass(type));
    }

}
