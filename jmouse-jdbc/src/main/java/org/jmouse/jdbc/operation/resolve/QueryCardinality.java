package org.jmouse.jdbc.operation.resolve;

/**
 * Declares the expected result cardinality of a resolved SQL query.
 */
public enum QueryCardinality {

    /**
     * Zero or more rows.
     */
    LIST,

    /**
     * Zero or one row.
     */
    OPTIONAL,

    /**
     * Exactly one row.
     */
    SINGLE

}