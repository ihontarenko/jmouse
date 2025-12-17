package org.jmouse.jdbc.mapping;

import java.sql.ResultSet;
import java.sql.SQLException;

@FunctionalInterface
public interface ResultSetExtractor<T> {
    T extract(ResultSet rs) throws SQLException;
}
