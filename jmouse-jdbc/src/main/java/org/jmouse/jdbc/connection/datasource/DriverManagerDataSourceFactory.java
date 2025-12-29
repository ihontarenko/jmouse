package org.jmouse.jdbc.connection.datasource;

import javax.sql.DataSource;

public final class DriverManagerDataSourceFactory implements DataSourceFactory {

    @Override
    public boolean supports(DataSourceSpecification spec) {
        return true;
    }

    @Override
    public DataSource create(DataSourceSpecification specification) {
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
