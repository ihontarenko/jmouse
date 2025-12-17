package org.jmouse.jdbc.bind;

public interface SqlParameterSource {

    boolean hasValue(String name);

    Object getValue(String name);

}
