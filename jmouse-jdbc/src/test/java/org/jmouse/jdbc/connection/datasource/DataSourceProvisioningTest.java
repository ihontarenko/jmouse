package org.jmouse.jdbc.connection.datasource;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sql.DataSource;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;

class DataSourceProvisioningTest {

    @Test
    void namedSourcesNeedOnlyOneRegistry() {
        DataSourceRegistry registry = new DataSourceRegistry();
        DataSource source = new JdbcDataSource();
        registry.register("primary", source);
        assertSame(source, registry.resolve("primary"));
        assertThrows(IllegalStateException.class, () -> registry.resolve("missing"));
        assertThrows(IllegalStateException.class, () -> registry.register("primary", source));
        assertThrows(IllegalArgumentException.class, () -> registry.resolve(" "));
    }

    @Test
    void concurrentResolutionCreatesOneSource() throws Exception {
        DataSourceRegistry registry = new DataSourceRegistry();
        AtomicInteger created = new AtomicInteger();
        registry.registerLazy("tenant", () -> {
            created.incrementAndGet();
            return new JdbcDataSource();
        });
        assertEquals(0, created.get());
        try (var executor = Executors.newFixedThreadPool(8)) {
            List<Callable<DataSource>> requests = new ArrayList<>();
            for (int index = 0; index < 32; index++) {
                requests.add(() -> registry.resolve("tenant"));
            }
            var results = executor.invokeAll(requests, 5, TimeUnit.SECONDS);
            DataSource first = results.getFirst().get();
            for (var result : results) {
                assertSame(first, result.get());
            }
        }
        assertEquals(1, created.get());
    }

    @Test
    void failedCreationCanRetryWithoutPoisoningRegistration() {
        DataSourceRegistry registry = new DataSourceRegistry();
        AtomicInteger attempts = new AtomicInteger();
        registry.registerLazy("tenant", () -> {
            if (attempts.incrementAndGet() == 1) {
                throw new IllegalStateException("temporary factory failure");
            }
            return new JdbcDataSource();
        });
        assertThrows(IllegalStateException.class, () -> registry.resolve("tenant"));
        assertSame(registry.resolve("tenant"), registry.resolve("tenant"));
        assertEquals(2, attempts.get());
    }

    @Test
    void supportsChecksActualCapabilityAndDoesNotConstruct() {
        AtomicInteger created = new AtomicInteger();
        DataSourceFactoryRegistry factories = new DataSourceFactoryRegistry().register(
            factory(false, 1, created)
        );
        assertFalse(factories.supports(specification(null, null, null)));
        assertEquals(0, created.get());
        assertThrows(IllegalStateException.class, () -> factories.create(specification(null, null, null)));
    }

    @Test
    void highestPriorityWinsAndBestPriorityTiesAreRejected() {
        AtomicInteger lower = new AtomicInteger();
        AtomicInteger higher = new AtomicInteger();
        DataSourceFactoryRegistry factories = new DataSourceFactoryRegistry()
            .register(factory(true, 1, lower))
            .register(factory(true, 2, higher));
        factories.create(specification(null, null, null));
        assertEquals(0, lower.get());
        assertEquals(1, higher.get());
        factories.register(factory(true, 2, new AtomicInteger()));
        assertThrows(IllegalStateException.class, () -> factories.create(specification(null, null, null)));
    }

    @Test
    void unpooledFactoriesNeverSilentlyIgnorePoolConfiguration() {
        var pool = new DataSourceSpecification.Pool(
            1,
            4,
            Duration.ofSeconds(1),
            Duration.ofMinutes(1),
            Duration.ofMinutes(10)
        );
        var specification = specification(null, pool, null);
        DataSourceFactoryRegistry factories = new DataSourceFactoryRegistry()
            .register(new DriverManagerDataSourceFactory())
            .register(new DriverInstanceDataSourceFactory());
        assertFalse(factories.supports(specification));
        assertThrows(IllegalStateException.class, () -> factories.create(specification));
    }

    @Test
    void explicitDriverIsConstructedAndReceivesSchemaAndProperties() throws Exception {
        Properties properties = new Properties();
        properties.setProperty("MODE", "PostgreSQL");
        DataSource source = new DriverInstanceDataSourceFactory().create(
            specification("org.h2.Driver", null, properties)
        );
        try (var connection = source.getConnection()) {
            assertEquals("PUBLIC", connection.getSchema());
            try (
                var statement = connection.createStatement();
                var rows = statement.executeQuery(
                    "select setting_value from information_schema.settings where setting_name='MODE'"
                )
            ) {
                assertTrue(rows.next());
                assertEquals("PostgreSQL", rows.getString(1));
            }
        }
    }

    @Test
    void invalidDriverKeepsTheOriginalCause() {
        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class, () ->
            new DriverInstanceDataSourceFactory().create(specification("missing.Driver", null, null))
        );
        assertInstanceOf(ClassNotFoundException.class, failure.getCause());
        assertThrows(IllegalArgumentException.class, () ->
            new DriverInstanceDataSourceFactory().create(specification("java.lang.String", null, null))
        );
    }

    @Test
    void driverThatDoesNotAcceptUrlFailsWithSqlException() {
        DriverInstanceDataSource source = new DriverInstanceDataSource(new org.h2.Driver());
        source.setUrl("jdbc:unknown:database");
        assertThrows(java.sql.SQLException.class, source::getConnection);
    }

    private DataSourceSpecification specification(
        String driver,
        DataSourceSpecification.Pool pool,
        Properties properties
    ) {
        return new DataSourceSpecification(
            "test",
            driver,
            "jdbc:h2:mem:provisioning",
            "sa",
            "",
            null,
            "PUBLIC",
            pool,
            properties
        );
    }

    private DataSourceFactory factory(boolean supported, int priority, AtomicInteger created) {
        return new DataSourceFactory() {
            public boolean supports(DataSourceSpecification specification) {
                return supported;
            }

            public DataSource create(DataSourceSpecification specification) {
                created.incrementAndGet();
                return new JdbcDataSource();
            }

            public int priority() {
                return priority;
            }
        };
    }
}
