package org.jmouse.jdbc.parameters;

import java.util.List;
import java.util.Objects;

public final class ListParameterSource implements ParameterSource {

    private final List<?> parameterValues;

    public ListParameterSource(List<?> parameterValues) {
        this.parameterValues = Objects.requireNonNull(parameterValues, "parameterValues");
    }

    @Override
    public boolean hasValue(int position) {
        int index = position - 1;
        return index >= 0 && index < parameterValues.size();
    }

    @Override
    public boolean hasValue(String name) {
        return false;
    }

    @Override
    public Object getValue(int position) {
        return parameterValues.get(position - 1);
    }

    @Override
    public Object getValue(String name) {
        throw new UnsupportedOperationException("Named parameters are not supported by ListParameterSource");
    }
}
