package org.jmouse.jdbc.connection.datasource;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.Properties;
import org.jmouse.jdbc.connection.datasource.support.AbstractDriverBasedDataSource;

public class DriverInstanceDataSource extends AbstractDriverBasedDataSource {

    private final Driver driver;

    public DriverInstanceDataSource(Driver driver) {
        this.driver = java.util.Objects.requireNonNull(driver, "driver");
    }

    @Override
    protected Connection getConnectionFor(Properties properties) throws SQLException {
        Connection connection = driver.connect(getUrl(), properties);
        if (connection == null) {
            throw new SQLException("Configured JDBC driver does not accept this URL");
        }
        return connection;
    }
}
