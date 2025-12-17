package org.jmouse.jdbc.platform;

import org.jmouse.jdbc.connection.ConnectionProvider;

import java.util.Objects;

public final class DriverManagerPlatform implements JdbcPlatform {

    private final DriverManagerPlatformConfig config;
    private final DialectInputs dialectInputs;
    private final PlatformCapabilities capabilities;

    public DriverManagerPlatform(
            DriverManagerPlatformConfig config,
            DialectInputs dialectInputs,
            PlatformCapabilities capabilities
    ) {
        this.config = config;
        this.dialectInputs = dialectInputs;
        this.capabilities = capabilities;
    }

    public static DriverManagerPlatform of(DriverManagerPlatformConfig config, DialectInputs dialectInputs) {
        return new DriverManagerPlatform(config, dialectInputs, PlatformCapabilities.defaults());
    }

    @Override
    public ConnectionProvider connectionProvider() {
        return new DriverManagerConnectionProvider(config);
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
