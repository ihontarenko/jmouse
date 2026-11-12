package org.jmouse.jdbc.connection.datasource;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import org.jmouse.jdbc.connection.datasource.support.AbstractDriverBasedDataSource;
import org.junit.jupiter.api.Test;

class DriverDataSourceLifecycleTest {

    @Test
    void failedSchemaSetupClosesConnectionAndPreservesCloseFailure() {
        SQLException original = new SQLException("schema setup failed");
        SQLException closeFailure = new SQLException("close failed");
        AtomicInteger closed = new AtomicInteger();
        AbstractDriverBasedDataSource failing = new AbstractDriverBasedDataSource() {
            protected Connection getConnectionFor(Properties properties) {
                return (Connection) Proxy.newProxyInstance(
                    Connection.class.getClassLoader(),
                    new Class<?>[] { Connection.class },
                    (proxy, method, arguments) -> {
                        if (method.getName().equals("setSchema")) {
                            throw original;
                        }
                        if (method.getName().equals("close")) {
                            closed.incrementAndGet();
                            throw closeFailure;
                        }
                        return null;
                    }
                );
            }
        };
        failing.setUrl("jdbc:test");
        failing.setSchema("invalid");
        assertSame(original, assertThrows(SQLException.class, failing::getConnection));
        assertEquals(1, closed.get());
        assertArrayEquals(new Throwable[] { closeFailure }, original.getSuppressed());
    }

    @Test
    void connectionPropertiesAreDefensivelyCopiedIncludingDefaults() throws Exception {
        Properties defaults = new Properties();
        defaults.setProperty("application", "initial");
        Properties supplied = new Properties(defaults);
        supplied.setProperty("user", "configured");
        Properties[] observed = new Properties[1];
        AbstractDriverBasedDataSource source = new AbstractDriverBasedDataSource() {
            protected Connection getConnectionFor(Properties properties) throws SQLException {
                observed[0] = properties;
                return new org.h2.Driver().connect("jdbc:h2:mem:propertycopies", new Properties());
            }
        };
        source.setUrl("jdbc:h2:mem:propertycopies");
        source.setConnectionProperties(supplied);
        defaults.setProperty("application", "changed");
        supplied.setProperty("user", "changed");
        source.getConnectionProperties().setProperty("user", "changed again");
        try (var connection = source.getConnection("explicit", "password")) {
            assertFalse(connection.isClosed());
        }
        assertEquals("initial", observed[0].getProperty("application"));
        assertEquals("explicit", observed[0].getProperty("user"));
        assertEquals("password", observed[0].getProperty("password"));
    }
}
