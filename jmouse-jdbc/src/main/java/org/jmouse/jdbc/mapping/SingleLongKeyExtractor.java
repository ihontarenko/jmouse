package org.jmouse.jdbc.mapping;

import java.sql.ResultSet;
import java.sql.SQLException;

public final class SingleLongKeyExtractor implements KeyExtractor<Long> {

    @Override
    public Long extract(ResultSet keys) throws SQLException {
        return keys.next() ? keys.getLong(1) : null;
    }

}
