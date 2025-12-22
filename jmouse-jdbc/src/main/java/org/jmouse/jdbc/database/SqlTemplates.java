package org.jmouse.jdbc.database;

public interface SqlTemplates {

    String limitOffset(String sql, int offset, int limit);

    /**
     * Optional: next sequence value (if supported).
     */
    default String nextSequenceValue(String sequence) {
        throw new UnsupportedOperationException("Sequences are not supported by this platform");
    }

    /**
     * Optional: identity / generated keys specifics can go here later (Stage 8.*).
     */
}
