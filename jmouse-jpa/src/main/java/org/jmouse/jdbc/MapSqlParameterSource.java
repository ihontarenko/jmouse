package org.jmouse.jdbc;

import java.util.Map;

/**
 * 🗺️ Map-backed named parameters.
 */
public final class MapSqlParameterSource implements SqlParameterSource {

    private final Map<String, ?> parameters;

    public MapSqlParameterSource(Map<String, ?> parameters) {
        this.parameters = parameters;
    }

    @Override
    public boolean has(String name) {
        return parameters.containsKey(name);
    }

    @Override
    public Object get(String name) {
        if (!parameters.containsKey(name)) {
            throw new IllegalArgumentException("Missing named param: " + name);
        }
        return parameters.get(name);
    }
}