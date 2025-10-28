package org.jmouse.jdbc.core;

import javax.sql.DataSource;

/**
 * 🧩 Provides a {@link DataSource}.
 */
public interface DataSourceProvider {

    static DataSourceProvider of(DataSource dataSource) {
        return () -> dataSource;
    }

    DataSource get();
}
