package org.jmouse.jdbc.mapping;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.function.Consumer;

/**
 * {@link ResultSetExtractor} implementation for <b>streaming</b> row-by-row processing.
 * <p>
 * {@code StreamingResultSetExtractor} iterates through the {@link ResultSet}
 * and forwards each mapped row to a provided {@link Consumer} without
 * accumulating results in memory.
 *
 * <p>
 * This extractor is suitable for:
 * <ul>
 *     <li>large result sets</li>
 *     <li>ETL-style processing</li>
 *     <li>side-effect driven consumption (logging, aggregation, IO)</li>
 * </ul>
 *
 * <h3>Row index semantics</h3>
 * <p>
 * The {@code rowIndex} passed to the {@link RowMapper} is:
 * <ul>
 *     <li>0-based</li>
 *     <li>incremented sequentially for each processed row</li>
 * </ul>
 *
 * <h3>Example</h3>
 * <pre>{@code
 * jdbc.query(
 *     "select * from events",
 *     new StreamingResultSetExtractor<>(
 *         (rs, i) -> rs.getString("payload"),
 *         payload -> System.out.println(payload)
 *     )
 * );
 * }</pre>
 *
 * <p>
 * Note: The {@link ResultSet} lifecycle is managed by the executor/template.
 * Implementations must not close the result set.
 *
 * @param <T> mapped element type
 *
 * @author jMouse
 */
public final class StreamingResultSetExtractor<T> implements ResultSetExtractor<Void> {

    /**
     * Delegate mapperProvider used to map each row.
     */
    private final RowMapper<T> mapper;

    /**
     * Consumer invoked for each mapped row.
     */
    private final Consumer<T> consumer;

    /**
     * Creates a new {@code StreamingResultSetExtractor}.
     *
     * @param mapper   row mapperProvider used for each result row
     * @param consumer consumer invoked per mapped row
     */
    public StreamingResultSetExtractor(RowMapper<T> mapper, Consumer<T> consumer) {
        this.mapper = mapper;
        this.consumer = consumer;
    }

    /**
     * Iterates through the {@link ResultSet}, mapping and consuming each row.
     *
     * @param resultSet JDBC result set positioned before the first row
     * @return {@code null} (streaming extractor does not produce a value)
     * @throws SQLException if JDBC access fails during iteration or mapping
     */
    @Override
    public Void extract(ResultSet resultSet) throws SQLException {
        int rowIndex = 0;

        while (resultSet.next()) {
            consumer.accept(mapper.map(resultSet, rowIndex++));
        }

        return null;
    }
}
