package org.jmouse.jdbc.platform;

import org.jmouse.core.Contract;
import org.jmouse.jdbc.connection.ConnectionProvider;

import javax.sql.DataSource;

public final class DataSourcePlatform implements JdbcPlatform {

    private final DataSource           dataSource;
    private final DialectInputs        dialectInputs;
    private final PlatformCapabilities capabilities;

    public DataSourcePlatform(
            DataSource dataSource,
            DialectInputs dialectInputs,
            PlatformCapabilities capabilities
    ) {
        this.dataSource = Contract.nonNull(dataSource, "dataSource");
        this.dialectInputs = Contract.nonNull(dialectInputs, "dialectInputs");
        this.capabilities = Contract.nonNull(capabilities, "capabilities");
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
