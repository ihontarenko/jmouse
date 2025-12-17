package org.jmouse.jdbc.dialect;

public interface SqlDialect {

    String limit(String sql, int offset, int limit);

    String sequenceNextValue(String sequence);

}