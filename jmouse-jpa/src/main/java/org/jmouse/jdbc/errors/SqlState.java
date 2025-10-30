package org.jmouse.jdbc.errors;

/**
 * 🧩 Simple SQLState classification constants.
 */
public final class SqlState {

    public static final String UNIQUE_VIOLATION = "23505";
    public static final String FK_VIOLATION     = "23503";
    public static final String SERIALIZATION    = "40001";
    public static final String DEADLOCK         = "40P01";

    private SqlState() {
    }

}