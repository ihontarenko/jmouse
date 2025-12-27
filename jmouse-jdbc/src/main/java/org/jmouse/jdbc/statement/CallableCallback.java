package org.jmouse.jdbc.statement;

import java.sql.CallableStatement;
import java.sql.SQLException;

public interface CallableCallback<T> {
    T doInCallable(CallableStatement callableStatement) throws SQLException;
}
