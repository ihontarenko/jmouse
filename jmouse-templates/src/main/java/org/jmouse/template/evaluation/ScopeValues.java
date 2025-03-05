package org.jmouse.template.evaluation;

public interface ScopeValues {

    Object get(String name);

    void set(String name, Object value);

    default boolean contains(String name) {
        return get(name) != null;
    }

}
