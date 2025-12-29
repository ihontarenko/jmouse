package org.jmouse.jdbc.parameters;

import java.util.Objects;

public final class ArrayParameterSource implements ParameterSource {

    private final Object[] arrayValues;

    public ArrayParameterSource(Object[] arrayValues) {
        this.arrayValues = Objects.requireNonNull(arrayValues, "arrayValues");
    }

    @Override
    public boolean hasValue(int position) {
        int index = position - 1;
        return index >= 0 && index < arrayValues.length;
    }

    @Override
    public boolean hasValue(String name) {
        return false;
    }

    @Override
    public Object getValue(int position) {
        return arrayValues[position - 1];
    }

    @Override
    public Object getValue(String name) {
        throw new UnsupportedOperationException("Named parameters are not supported by ArrayParameterSource");
    }
}
