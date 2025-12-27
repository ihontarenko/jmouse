package org.jmouse.jdbc.intercept;

public sealed interface JdbcCall<R>
        permits JdbcQueryCall, JdbcUpdateCall, JdbcBatchUpdateCall, JdbcKeyUpdateCall, JdbcCallableCall {
    String sql();
    JdbcOperation operation();
}
