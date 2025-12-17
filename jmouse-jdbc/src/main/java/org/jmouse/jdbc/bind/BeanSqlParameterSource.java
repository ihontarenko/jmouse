package org.jmouse.jdbc.bind;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class BeanSqlParameterSource implements ParameterSource {

    private final Object bean;
    private final Map<String, Method> getters;

    public BeanSqlParameterSource(Object bean) {
        this.bean = Objects.requireNonNull(bean, "bean");
        this.getters = introspect(bean.getClass());
    }

    @Override
    public boolean hasValue(String name) {
        return getters.containsKey(name);
    }

    @Override
    public Object getValue(String name) {
        Method getter = getters.get(name);
        if (getter == null) return null;
        try {
            return getter.invoke(bean);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to read parameter '" + name + "' from " + bean.getClass().getName(), e);
        }
    }

    private static Map<String, Method> introspect(Class<?> type) {
        Map<String, Method> map = new HashMap<>();
        for (Method m : type.getMethods()) {
            if (m.getParameterCount() != 0) continue;
            if (m.getReturnType() == void.class) continue;

            String name = m.getName();
            if (name.equals("getClass")) continue;

            String prop = null;
            if (name.startsWith("get") && name.length() > 3) {
                prop = decap(name.substring(3));
            } else if (name.startsWith("is") && name.length() > 2 &&
                    (m.getReturnType() == boolean.class || m.getReturnType() == Boolean.class)) {
                prop = decap(name.substring(2));
            }

            if (prop != null && !prop.isEmpty()) {
                map.putIfAbsent(prop, m);
            }
        }
        return map;
    }

    private static String decap(String s) {
        if (s.length() == 1) return s.toLowerCase();
        return Character.toLowerCase(s.charAt(0)) + s.substring(1);
    }
}
