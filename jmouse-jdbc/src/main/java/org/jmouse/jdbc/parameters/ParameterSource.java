package org.jmouse.jdbc.parameters;

public interface ParameterSource {

    boolean hasValue(int position);

    boolean hasValue(String name);

    Object getValue(int position);

    Object getValue(String name);

}
