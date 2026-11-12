package org.jmouse.jdbc.exception;

/** Database rejected a transaction, for example because of a deadlock or serialization failure. */
public final class TransactionRollbackException extends DataAccessException {

    public TransactionRollbackException(String task, String sql, Throwable cause) {
        super("Database transaction rolled back during " + task + " for SQL [" + sql + "]", cause);
    }
}
