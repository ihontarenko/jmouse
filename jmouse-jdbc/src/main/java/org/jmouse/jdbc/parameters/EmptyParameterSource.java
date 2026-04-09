package org.jmouse.jdbc.parameters;

/**
 * Empty {@link ParameterSource} implementation.
 *
 * @author Ivan Hontarenko
 */
public final class EmptyParameterSource implements ParameterSource {

    public static final EmptyParameterSource INSTANCE = new EmptyParameterSource();

    private EmptyParameterSource() {
    }

    @Override
    public boolean hasValue(int position) {
        return false;
    }

    @Override
    public boolean hasValue(String name) {
        return false;
    }

    @Override
    public Object getValue(int position) {
        throw new UnsupportedOperationException("No positional parameters available");
    }

    @Override
    public Object getValue(String name) {
        throw new UnsupportedOperationException("No named parameters available");
    }

}