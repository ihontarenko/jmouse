package org.jmouse.jdbc.config.spi;

import org.jmouse.jdbc.config.DataSourceProperties;

import javax.sql.DataSource;

/**
 * 🔌 Pluggable DataSource provider (pool or plain).
 * Implementations are discovered via ServiceLoader.
 */
public interface DataSourceProvider {
    /**
     * A short id, e.g. "hikari", "dbcp2", "tomcat", "driver" (fallback).
     */
    String id();

    /**
     * Return true if this provider is usable with current classpath & sourceProperties.
     */
    boolean isAvailable(DataSourceProperties sourceProperties);

    /**
     * Create configured DataSource from sourceProperties.
     */
    DataSource create(DataSourceProperties sourceProperties);
}