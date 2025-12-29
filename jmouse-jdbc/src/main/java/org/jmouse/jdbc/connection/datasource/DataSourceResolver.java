package org.jmouse.jdbc.connection.datasource;

import javax.sql.DataSource;

public interface DataSourceResolver {
    DataSource resolve(String name);
}