package org.jmouse.jdbc;

import org.jmouse.jdbc.bootstrap.JdbcBootstrap;
import org.jmouse.jdbc.bootstrap.JdbcClient;
import org.jmouse.jdbc.bootstrap.JdbcConfig;
import org.jmouse.jdbc.platform.DialectInputs;
import org.jmouse.jdbc.platform.DriverManagerPlatform;
import org.jmouse.jdbc.platform.DriverManagerPlatformConfig;

import java.sql.SQLException;

public class Test {

    public static void main(String[] args) throws SQLException {
        JdbcConfig config = JdbcConfig.defaults()
                .withDialectId("postgres");

        DriverManagerPlatform platform = DriverManagerPlatform.of(
                DriverManagerPlatformConfig.ofCredentials("", "admin", "password"),
                DialectInputs.configured("postgres", true)
        );

        JdbcClient client = JdbcBootstrap.create(config, platform);

        client.jdbc().querySingle("select 1", rs -> rs.getInt(1));
    }

}
