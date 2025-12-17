package org.jmouse.jdbc.dialect;

public interface DialectResolver {

    /**
     * Resolve current dialect id (e.g. from config, env, or connection metadata).
     */
    String resolveDialectId();
}
