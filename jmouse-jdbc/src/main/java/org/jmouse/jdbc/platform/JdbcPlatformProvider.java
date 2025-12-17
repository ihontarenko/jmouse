package org.jmouse.jdbc.platform;

/**
 * Factory/provider for JdbcPlatform instances.
 */
@FunctionalInterface
public interface JdbcPlatformProvider {

    JdbcPlatform get();

}
