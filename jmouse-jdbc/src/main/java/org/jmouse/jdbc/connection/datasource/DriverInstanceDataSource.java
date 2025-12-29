package org.jmouse.jdbc.connection.datasource;

import org.jmouse.jdbc.connection.datasource.support.AbstractDriverBasedDataSource;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.Properties;

public class DriverInstanceDataSource extends AbstractDriverBasedDataSource {

    private final Driver driver;

    public DriverInstanceDataSource(Driver driver) {
        this.driver = driver;
    }

    @Override
    protected Connection getConnectionFor(Properties properties) throws SQLException {
        return driver.connect(getUrl(), properties);
    }

}
