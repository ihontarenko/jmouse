package org.jmouse.jdbc.connection.datasource;

import javax.sql.DataSource;

public class DriverInstanceDataSourceFactory implements DataSourceFactory {

    @Override
    public boolean supports(DataSourceSpecification specification) {
        return specification.driverClassName() != null && !specification.driverClassName().isEmpty();
    }

    @Override
    public DataSource create(DataSourceSpecification specification) {
        DriverInstanceDataSource dataSource = new DriverInstanceDataSource(
                Class.forName(specification.driverClassName())
        );

        return dataSource;
    }

}
