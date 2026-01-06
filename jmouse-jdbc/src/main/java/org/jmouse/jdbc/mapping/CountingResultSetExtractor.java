package org.jmouse.jdbc.mapping;

import org.jmouse.jdbc.report.ExecutionReportCollector;

import java.sql.ResultSet;
import java.sql.SQLException;

public final class CountingResultSetExtractor<T> implements ResultSetExtractor<T> {

    private final ResultSetExtractor<T> delegate;
    private final ExecutionReportCollector collector;
    private long rows;

    public CountingResultSetExtractor(ResultSetExtractor<T> delegate, ExecutionReportCollector collector) {
        this.delegate = delegate;
        this.collector = collector;
    }

    @Override
    public T extract(ResultSet resultSet) throws SQLException {
        T result = delegate.extract(new CountingResultSet(resultSet));
        collector.rowsRead(((CountingResultSet) ((CountingResultSet) null)).count()); // (see below)
        return result;
    }

    /**
     * Minimal wrapper that counts next() calls.
     */
    private final class CountingResultSet extends ResultSetWrapper {
        private long count;

        CountingResultSet(ResultSet delegate) {
            super(delegate);
        }

        @Override
        public boolean next() throws SQLException {
            boolean has = super.next();
            if (has) count++;
            return has;
        }

        long count() {
            return count;
        }
    }
}
