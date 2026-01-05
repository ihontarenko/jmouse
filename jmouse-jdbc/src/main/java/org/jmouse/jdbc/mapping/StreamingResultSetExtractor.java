package org.jmouse.jdbc.mapping;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.function.Consumer;

public final class StreamingResultSetExtractor<T> implements ResultSetExtractor<Void> {

    private final RowMapper<T> mapper;
    private final Consumer<T>  consumer;

    public StreamingResultSetExtractor(RowMapper<T> mapper, Consumer<T> consumer) {
        this.mapper = mapper;
        this.consumer = consumer;
    }

    @Override
    public Void extract(ResultSet resultSet) throws SQLException {
        int rowIndex = 0;

        while (resultSet.next()) {
            consumer.accept(mapper.map(resultSet, rowIndex++));
        }

        return null;
    }
}
