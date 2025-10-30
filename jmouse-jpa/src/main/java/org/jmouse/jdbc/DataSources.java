package org.jmouse.jdbc;

import javax.sql.DataSource;
import java.sql.Connection;

final public class DataSources {

    private DataSources() {}

    public static Connection getConnection(DataSource dataSource) {
        return null;
    }

    public static void releaseConnection(Connection connection, DataSource dataSource) {

    }

}
