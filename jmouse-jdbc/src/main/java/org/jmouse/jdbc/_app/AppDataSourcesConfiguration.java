package org.jmouse.jdbc._app;

import org.jmouse.beans.annotation.Bean;
import org.jmouse.beans.annotation.BeanFactories;
import org.jmouse.beans.annotation.PrimaryBean;
import org.jmouse.core.Priority;
import org.jmouse.jdbc.connection.datasource.*;
import org.jmouse.jdbc.connection.datasource.ResolvableDataSource;

import javax.sql.DataSource;
import java.time.Duration;
import java.util.Properties;

@BeanFactories
public class AppDataSourcesConfiguration {

    @Bean
    @PrimaryBean
    public DataSource dataSource(DataSourceResolver resolver, DataSourceKey dataSourceKey) {
        return new ResolvableDataSource(resolver, dataSourceKey);
    }

    @Bean
    @Priority(0)
    public DataSourceContributor postgresDataSource() {
        return registry -> registry.register(
                new DataSourceSpecification(
                        "postgres",
                        "org.postgresql.Driver",
                        "jdbc:postgresql://localhost:5432/jmouse",
                        "jmouse",
                        "jmouse",
                        null,
                        null,
                        pool(1, 10),
                        properties()
                )
        );
    }

    @Bean
    @Priority(1)
    public DataSourceContributor mysqlDataSource() {
        Properties properties = properties(
                "useUnicode", "true",
                "characterEncoding", "utf8",
                "useSSL", "false",
                "allowPublicKeyRetrieval", "true"
        );

        return registry -> registry.register(
                new DataSourceSpecification(
                        "mysql",
                        "com.mysql.cj.jdbc.Driver",
                        "jdbc:mysql://localhost:3306/jmouse",
                        "jmouse",
                        "jmouse",
                        null,
                        null,
                        pool(1, 10),
                        properties
                )
        );
    }

    @Bean
    @Priority(2)
    public DataSourceContributor oracleDataSource() {
        return registry -> registry.register(
                new DataSourceSpecification(
                        "oracle",
                        "oracle.jdbc.OracleDriver",
                        "jdbc:oracle:thin:@localhost:1521/FREEPDB1",
                        "system",
                        "Oracle_123",
                        null,
                        null,
                        pool(1, 5),
                        properties()
                )
        );
    }

    @Bean
    @Priority(3)
    public DataSourceContributor h2DataSource() {
        Properties p = properties(
                "MODE", "PostgreSQL"
        );

        return registry -> registry.register(
                new DataSourceSpecification(
                        "h2",
                        "org.h2.Driver",
                        "jdbc:h2:tcp://localhost:9092/./jmouse;IFEXISTS=FALSE",
                        "sa",
                        "",
                        null,
                        null,
                        pool(1, 5),
                        p
                )
        );
    }

    private static DataSourceSpecification.Pool pool(int min, int max) {
        return new DataSourceSpecification.Pool(
                min,
                max,
                Duration.ofSeconds(10),
                Duration.ofMinutes(5),
                Duration.ofMinutes(30)
        );
    }

    private static Properties properties(String... kv) {
        Properties properties = new Properties();

        if (kv == null) {
            return properties;
        }

        if ((kv.length % 2) != 0) {
            throw new IllegalArgumentException("props requires even key/value args");
        }

        for (int i = 0; i < kv.length; i += 2) {
            properties.setProperty(kv[i], kv[i + 1]);
        }

        return properties;
    }

    private static Properties properties() {
        return new Properties();
    }
}
