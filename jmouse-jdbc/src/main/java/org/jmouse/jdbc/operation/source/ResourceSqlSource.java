package org.jmouse.jdbc.operation.source;

import static org.jmouse.core.Verify.notBlank;

/**
 * Classpath resource-based SQL source.
 *
 * @author Ivan Hontarenko
 */
public record ResourceSqlSource(String value) implements SqlSource {

    /**
     * Creates a new resource SQL source.
     *
     * @param value classpath SQL resource location
     */
    public ResourceSqlSource {
        notBlank(value, "value");
    }

}