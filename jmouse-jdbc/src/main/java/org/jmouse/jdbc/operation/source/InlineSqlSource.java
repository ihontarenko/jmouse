package org.jmouse.jdbc.operation.source;

import static org.jmouse.core.Verify.notBlank;

/**
 * Inline SQL source.
 *
 * @author Ivan Hontarenko
 */
public record InlineSqlSource(String value) implements SqlSource {

    /**
     * Creates a new inline SQL source.
     *
     * @param value inline SQL text
     */
    public InlineSqlSource {
        notBlank(value, "value");
    }

}