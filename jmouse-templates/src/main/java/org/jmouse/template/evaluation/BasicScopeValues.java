package org.jmouse.template.evaluation;

import java.util.HashMap;
import java.util.Map;

public class BasicScopeValues implements ScopeValues {

    private final Map<String, Object> values = new HashMap<>();

    @Override
    public Object get(String name) {
        return values.get(name);
    }

    @Override
    public void set(String name, Object value) {
        values.put(name, value);
    }
}
