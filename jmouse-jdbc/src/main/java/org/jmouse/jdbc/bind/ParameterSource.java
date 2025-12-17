package org.jmouse.jdbc.bind;

public interface ParameterSource {

    boolean hasValue(String name);

    Object getValue(String name);

}
