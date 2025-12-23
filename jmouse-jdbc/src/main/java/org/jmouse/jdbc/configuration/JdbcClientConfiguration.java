package org.jmouse.jdbc.configuration;

import org.jmouse.beans.annotation.Bean;
import org.jmouse.beans.annotation.BeanFactories;
import org.jmouse.jdbc.JdbcClient;
import org.jmouse.jdbc.core.JdbcOperations;
import org.jmouse.jdbc.database.DatabasePlatform;

@BeanFactories
public class JdbcClientConfiguration {

    @Bean
    public JdbcClient jdbcClient(JdbcOperations jdbcOperations, DatabasePlatform platform) {
        return new JdbcClient(jdbcOperations, platform);
    }

}
