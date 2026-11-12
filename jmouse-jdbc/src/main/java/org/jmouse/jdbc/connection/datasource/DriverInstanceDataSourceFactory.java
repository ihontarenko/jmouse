package org.jmouse.jdbc.connection.datasource;

import java.sql.Driver;
import javax.sql.DataSource;

/** Explicit-driver alternative to JDBC service discovery. Does not implement connection pooling. */
public class DriverInstanceDataSourceFactory implements DataSourceFactory {

    @Override
    public boolean supports(DataSourceSpecification specification) {
        return (
            specification.pool() == null &&
            specification.url() != null &&
            !specification.url().isBlank() &&
            specification.driverClassName() != null &&
            !specification.driverClassName().isBlank()
        );
    }

    @Override
    public DataSource create(DataSourceSpecification specification) {
        if (!supports(specification)) {
            throw new IllegalArgumentException(
                "Unsupported driver DataSource specification: " + specification.name()
            );
        }
        DriverInstanceDataSource dataSource = new DriverInstanceDataSource(
            instantiateDriver(specification.driverClassName())
        );
        dataSource.setUrl(specification.url());
        dataSource.setUsername(specification.username());
        dataSource.setPassword(specification.password());
        dataSource.setCatalog(specification.catalog());
        dataSource.setSchema(specification.schema());
        dataSource.setConnectionProperties(specification.properties());
        return dataSource;
    }

    private Driver instantiateDriver(String driverClassName) {
        try {
            return Class.forName(driverClassName)
                .asSubclass(Driver.class)
                .getDeclaredConstructor()
                .newInstance();
        } catch (ReflectiveOperationException | ClassCastException exception) {
            throw new IllegalArgumentException(
                "Cannot instantiate JDBC driver: " + driverClassName,
                exception
            );
        }
    }
}
