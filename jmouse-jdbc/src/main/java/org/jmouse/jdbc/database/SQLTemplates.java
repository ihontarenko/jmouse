package org.jmouse.jdbc.database;

public interface SQLTemplates {

    String limitOffset(String sql, int offset, int limit);

    default String nextSequenceValue(String sequence) {
        throw new UnsupportedOperationException("Sequences are not supported by this platform");
    }

}
