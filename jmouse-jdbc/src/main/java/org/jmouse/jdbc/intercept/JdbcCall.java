package org.jmouse.jdbc.intercept;

public sealed interface JdbcCall<R>
        permits JdbcQueryCall, JdbcUpdateCall, JdbcBatchUpdateCall, JdbcKeyUpdateCall {
    String sql();
    JdbcOperation operation();
}
