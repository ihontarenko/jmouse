package org.jmouse.jdbc.transaction;

import org.jmouse.transaction.TransactionIsolation;

import java.sql.Connection;

public class JdbcIsolation {

    private JdbcIsolation() {}

    public static int toJdbc(TransactionIsolation isolation) {
        return switch (isolation) {
            case SERIALIZABLE -> Connection.TRANSACTION_SERIALIZABLE;
            case DEFAULT, READ_COMMITTED -> Connection.TRANSACTION_READ_COMMITTED;
            case READ_UNCOMMITTED -> Connection.TRANSACTION_READ_UNCOMMITTED;
            case REPEATABLE_READ -> Connection.TRANSACTION_REPEATABLE_READ;
        };
    }

    public static boolean isDefault(TransactionIsolation isolation) {
        return isolation == null || isolation == TransactionIsolation.DEFAULT;
    }

    public static boolean isNone(int jdbcIsolation) {
        return jdbcIsolation == Connection.TRANSACTION_NONE;
    }

}
