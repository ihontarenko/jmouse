package org.jmouse.jdbc.connection.datasource;

public interface DataSourceContributor {
    void contribute(DataSourceSpecificationRegistry registry);
}