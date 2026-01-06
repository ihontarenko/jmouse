package org.jmouse.jdbc.query;

import org.jmouse.jdbc.mapping.ResultSetExtractor;
import org.jmouse.jdbc.mapping.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class PageExtractors {

    private PageExtractors() {}

    public static <T> ResultSetExtractor<Page<T>> offsetLimit(
            OffsetLimit offsetLimit,
            RowMapper<T> mapper
    ) {
        return resultSet -> extract(offsetLimit, mapper, resultSet);
    }

    private static <T> Page<T> extract(
            OffsetLimit offsetLimit, RowMapper<T> mapper, ResultSet resultSet
    ) throws SQLException {
        int     fetch = offsetLimit.limit() + 1;
        List<T> list  = new ArrayList<>(Math.min(offsetLimit.limit(), 128));
        int     index = 0;

        while (resultSet.next() && index < fetch) {
            list.add(mapper.map(resultSet, index));
            index++;
        }

        boolean hasNext = list.size() > offsetLimit.limit();

        if (hasNext) {
            list.removeLast();
        }

        return Page.of(list, offsetLimit.offset(), offsetLimit.limit(), hasNext, null);
    }
}
