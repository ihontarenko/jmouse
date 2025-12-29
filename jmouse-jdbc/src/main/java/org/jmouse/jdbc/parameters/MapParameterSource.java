package org.jmouse.jdbc.parameters;

import org.jmouse.core.Contract;

import java.util.Map;

public final class MapParameterSource implements ParameterSource {

    private final Map<String, ?> values;

    public MapParameterSource(Map<String, ?> values) {
        this.values = Contract.nonNull(values, "values");
    }

    @Override
    public boolean hasValue(int position) {
        return false;
    }

    @Override
    public boolean hasValue(String name) {
        return values.containsKey(name);
    }

    @Override
    public Object getValue(int position) {
        throw new UnsupportedOperationException("Positional parameters are not supported by MapParameterSource");
    }

    @Override
    public Object getValue(String name) {
        return values.get(name);
    }
}
