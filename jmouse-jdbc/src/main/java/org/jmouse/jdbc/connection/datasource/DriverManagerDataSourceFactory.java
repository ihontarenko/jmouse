package org.jmouse.jdbc.connection.datasource;

import javax.sql.DataSource;

public final class DriverManagerDataSourceFactory implements DataSourceFactory {

    @Override
    public boolean supports(DataSourceSpecification specification) {
        return (
            specification.pool() == null &&
            specification.url() != null &&
            !specification.url().isBlank() &&
            (specification.driverClassName() == null || specification.driverClassName().isBlank())
        );
    }

    @Override
    public DataSource create(DataSourceSpecification specification) {
        if (!supports(specification)) {
            throw new IllegalArgumentException(
                "Unsupported DriverManager specification: " + specification.name()
            );
        }
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setUrl(specification.url());
        dataSource.setUsername(specification.username());
        dataSource.setPassword(specification.password());
        dataSource.setCatalog(specification.catalog());
        dataSource.setSchema(specification.schema());
        dataSource.setConnectionProperties(specification.properties());
        return dataSource;
    }

    @Override
    public int priority() {
        return Integer.MIN_VALUE;
    }
}
