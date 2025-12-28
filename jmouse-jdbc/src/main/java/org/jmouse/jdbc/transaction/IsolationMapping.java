package org.jmouse.jdbc.transaction;

import org.jmouse.transaction.TransactionIsolation;

import java.sql.Connection;

public class IsolationMapping {

    public static int toJDBCIsolation(TransactionIsolation iso) {
        return switch (iso) {
            case DEFAULT, READ_COMMITTED -> Connection.TRANSACTION_READ_COMMITTED;
            case READ_UNCOMMITTED -> Connection.TRANSACTION_READ_UNCOMMITTED;
            case REPEATABLE_READ -> Connection.TRANSACTION_REPEATABLE_READ;
            case SERIALIZABLE -> Connection.TRANSACTION_SERIALIZABLE;
        };
    }

}
