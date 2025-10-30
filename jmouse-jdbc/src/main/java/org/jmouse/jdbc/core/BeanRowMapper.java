package org.jmouse.jdbc.core;

import org.jmouse.jdbc.core.mapping.RowMapper;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Constructor;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 🧱 Tiny POJO mapper using JavaBeans setters (no caching in Part 1).
 * Column labels map to properties by:
 * - exact match (case-insensitive)
 * - underscore_to_camel fallback
 */
public final class BeanRowMapper<T> implements RowMapper<T> {

    private final Class<T> type;

    public BeanRowMapper(Class<T> type) {
        this.type = type;
    }

    private static String toCamel(String value) {
        StringBuilder builder    = new StringBuilder(value.length());
        boolean       upperCased = false;

        for (char character : value.toCharArray()) {
            if (character == '_' || character == ' ') {
                upperCased = true;
                continue;
            }
            builder.append(upperCased ? Character.toUpperCase(character) : Character.toLowerCase(character));
            upperCased = false;
        }

        return builder.toString();
    }

    @Override
    public T map(ResultSet resultSet, int rowNumber) throws SQLException {
        try {

            // ObjectAccessor accessor = ObjectAccessor.wrapObject();
            // Binder binder = new Binder();

            Constructor<T> ctor = type.getDeclaredConstructor();
            ctor.setAccessible(true);
            T bean = ctor.newInstance();

            Map<String, PropertyDescriptor> props = new HashMap<>();
            for (PropertyDescriptor pd : Introspector.getBeanInfo(type).getPropertyDescriptors()) {
                if (pd.getWriteMethod() != null) props.put(pd.getName().toLowerCase(Locale.ROOT), pd);
            }

            ResultSetMetaData md   = resultSet.getMetaData();
            int               cols = md.getColumnCount();
            for (int i = 1; i <= cols; i++) {
                String             label = md.getColumnLabel(i);
                String             key   = label.toLowerCase(Locale.ROOT);
                PropertyDescriptor pd    = props.get(key);
                if (pd == null) {
                    key = toCamel(label).toLowerCase(Locale.ROOT);
                    pd = props.get(key);
                }
                if (pd != null) {
                    Object val    = resultSet.getObject(i);
                    var    setter = pd.getWriteMethod();
                    setter.setAccessible(true);
                    setter.invoke(bean, val);
                }
            }
            return bean;
        } catch (Exception e) {
            throw new SQLException("BeanRowMapper failed for " + type.getName(), e);
        }
    }
}
