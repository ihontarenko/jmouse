package org.jmouse.jdbc.core;

import java.sql.ResultSet;
import java.sql.SQLException;

@FunctionalInterface
public interface KeyExtractor<K> {

    /**
     * Extract key from generated keys ResultSet.
     * Implementations should call keys.next() if needed.
     */
    K extract(ResultSet keys) throws SQLException;

}
