package org.jmouse.jdbc.tx;

/**
 * 💼 Minimal JDBC transaction contract.
 */
public interface JdbcTransaction {

    void begin();

    void commit();

    void rollback();

    boolean isActive();

}