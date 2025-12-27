package org.jmouse.jdbc.bind;

import org.jmouse.core.Contract;

public final class CompositeParameterSource implements ParameterSource {

    private final ParameterSource named;
    private final ParameterSource positional;

    public CompositeParameterSource(ParameterSource named, ParameterSource positional) {
        this.named = named;
        this.positional = positional;
    }

    public static CompositeParameterSource of(ParameterSource named, ParameterSource positional) {
        return new CompositeParameterSource(named, positional);
    }

    @Override
    public boolean hasValue(int position) {
        return positional != null && positional.hasValue(position);
    }

    @Override
    public boolean hasValue(String name) {
        return named != null && named.hasValue(name);
    }

    @Override
    public Object getValue(int position) {
        Contract.nonNull(positional, "positional parameter-source is required");
        return positional.getValue(position);
    }

    @Override
    public Object getValue(String name) {
        Contract.nonNull(named, "named parameter-source is required");
        return named.getValue(name);
    }
}
