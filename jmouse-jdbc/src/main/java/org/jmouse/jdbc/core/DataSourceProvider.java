package org.jmouse.jdbc;

/** 🧩 Provides a {@link DataSource}. */
public interface DataSourceProvider {
    DataSource get();

    static DataSourceProvider of(DataSource ds) {
        Objects.requireNonNull(ds, "DataSource");
        return () -> ds;
    }
}
