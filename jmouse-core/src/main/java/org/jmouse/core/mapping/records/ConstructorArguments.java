package org.jmouse.core.mapping.records;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ConstructorArguments {

    private final Map<String, Object> values = new LinkedHashMap<>();

    public void put(String name, Object value) {
        values.put(name, value);
    }

    public boolean contains(String name) {
        return values.containsKey(name);
    }

    public Object get(String name) {
        return values.get(name);
    }

    public Map<String, Object> asMap() {
        return values;
    }

}
