package org.jmouse.jdbc.connection.datasource;

import org.jmouse.core.reflection.Reflections;

import javax.sql.DataSource;
import java.lang.reflect.Constructor;
import java.sql.Driver;

public class DriverInstanceDataSourceFactory implements DataSourceFactory {

    @Override
    public boolean supports(DataSourceSpecification specification) {
        return specification.driverClassName() != null && !specification.driverClassName().isEmpty();
    }

    @Override
    public DataSource create(DataSourceSpecification specification) {
        DriverInstanceDataSource dataSource = new DriverInstanceDataSource(
                instantiateDriver(specification.driverClassName())
        );
        dataSource.setUrl(specification.url());
        dataSource.setUsername(specification.username());
        dataSource.setPassword(specification.password());
        return dataSource;
    }

    private Driver instantiateDriver(String driverClassName) {
        Driver driver = null;

        try {
            @SuppressWarnings("unchecked")
            Class<? extends Driver> driverClass = (Class<? extends Driver>) Class.forName(driverClassName);
            Constructor<?>          constructor = Reflections.findFirstConstructor(driverClass);
            driver = (Driver) Reflections.instantiate(constructor);
        } catch (Exception ignore) { }

        return driver;
    }

}
