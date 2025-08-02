package org.jmouse.mvc;

import java.util.Map;

public record RouteMatch(String pattern, Map<String, Object> variables) {
    public Object getVariable(String name, Object defaultValue) {
        return variables.getOrDefault(name, defaultValue);
    }
}
