package org.jmouse.jdbc.core;

import javax.sql.DataSource;
import java.util.Objects;

/** 🧩 Provides a {@link DataSource}. */
public interface DataSourceProvider {

    DataSource get();

    static DataSourceProvider of(DataSource dataSource) {
        Objects.requireNonNull(dataSource, "DataSource");
        return () -> dataSource;
    }
}
