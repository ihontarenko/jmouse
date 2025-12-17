package org.jmouse.jdbc.bind;

import java.util.Map;

public final class MapSqlParameterSource implements SqlParameterSource {

    private final Map<String, ?> values;

    public MapSqlParameterSource(Map<String, ?> values) {
        this.values = values;
    }

    @Override
    public boolean hasValue(String name) {
        return values.containsKey(name);
    }

    @Override
    public Object getValue(String name) {
        return values.get(name);
    }

}
