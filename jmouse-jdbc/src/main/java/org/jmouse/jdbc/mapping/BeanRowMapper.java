package org.jmouse.jdbc.mapping;

import org.jmouse.core.Contract;
import org.jmouse.core.bind.Binder;
import org.jmouse.core.bind.DefaultBindingCallback;
import org.jmouse.core.bind.PropertyAccessor;
import org.jmouse.core.bind.descriptor.Describer;
import org.jmouse.core.bind.descriptor.structured.ObjectDescriptor;
import org.jmouse.core.bind.descriptor.structured.PropertyDescriptor;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class BeanRowMapper<T> implements RowMapper<T> {

    private static final Map<Class<?>, ObjectDescriptor<?>> DESCRIPTORS = new ConcurrentHashMap<>();

    private final Class<T>                        type;
    private final Map<String, PropertyBinding<T>> bindings;
    private final NameStrategy                    strategy;

    public BeanRowMapper(Class<T> type, NameStrategy strategy) {
        this.type = Contract.nonNull(type, "type");
        this.strategy = Contract.nonNull(strategy, "strategy");
        this.bindings = toBindings(type);
    }

    public static <T> BeanRowMapper<T> of(Class<T> type) {
        return new BeanRowMapper<>(type, NameStrategy.toCamel());
    }

    public static <T> BeanRowMapper<T> of(Class<T> type, NameStrategy strategy) {
        return new BeanRowMapper<>(type, strategy);
    }

    @Override
    public T map(ResultSetRowMetadata view) throws SQLException {

        Binder binder = Binder.with(view, new DefaultBindingCallback());

        System.out.println(view);

        return null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, PropertyBinding<T>> toBindings(Class<T> type) {
        ObjectDescriptor<T>             descriptor = (ObjectDescriptor<T>) DESCRIPTORS
                .computeIfAbsent(type, Describer::forObjectDescriptor);
        Map<String, PropertyBinding<T>> bindings   = new HashMap<>();

        for (Map.Entry<String, PropertyDescriptor<T>> entry : descriptor.getProperties().entrySet()) {
            String                propertyName = entry.getKey();
            PropertyDescriptor<T> property     = entry.getValue();
            bindings.put(propertyName, new PropertyBinding<>(property.getAccessor()));
        }

        return bindings;
    }

    private record PropertyBinding<T>(PropertyAccessor<T> accessor) {
    }

}
