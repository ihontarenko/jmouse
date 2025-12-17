package org.jmouse.jdbc.platform;

import org.jmouse.jdbc.connection.ConnectionProvider;

import javax.sql.DataSource;
import java.util.Objects;

public final class DataSourcePlatform implements JdbcPlatform {

    private final DataSource dataSource;
    private final DialectInputs dialectInputs;
    private final PlatformCapabilities capabilities;

    public DataSourcePlatform(
            DataSource dataSource,
            DialectInputs dialectInputs,
            PlatformCapabilities capabilities
    ) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
        this.dialectInputs = Objects.requireNonNull(dialectInputs, "dialectInputs");
        this.capabilities = Objects.requireNonNull(capabilities, "capabilities");
    }

    public static DataSourcePlatform of(DataSource dataSource, DialectInputs dialectInputs) {
        return new DataSourcePlatform(dataSource, dialectInputs, PlatformCapabilities.defaults());
    }

    @Override
    public ConnectionProvider connectionProvider() {
        return new DataSourceConnectionProvider(dataSource);
    }

    @Override
    public DialectInputs dialectInputs() {
        return dialectInputs;
    }

    @Override
    public PlatformCapabilities capabilities() {
        return capabilities;
    }
}
