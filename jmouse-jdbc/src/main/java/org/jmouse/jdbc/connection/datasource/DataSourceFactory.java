package org.jmouse.jdbc.connection.datasource;

import javax.sql.DataSource;

public interface DataSourceFactory {

    boolean supports(DataSourceSpecification specification);

    DataSource create(DataSourceSpecification specification);

    default int priority() { return 0; }

}
