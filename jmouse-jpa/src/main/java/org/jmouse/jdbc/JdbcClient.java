package org.jmouse.jdbc;

import java.util.List;
import java.util.Optional;

/**
 * 🎯 High-level JDBC API.
 */
public interface JdbcClient {

    int update(String sql, Object... params);

    int insert(String sql, KeyHolder keys, Object... params);

    <T> List<T> query(String sql, RowMapper<T> mapper, Object... params);

    <T> Optional<T> one(String sql, RowMapper<T> mapper, Object... params);

    <T> T required(String sql, RowMapper<T> mapper, Object... params);

    int[] batch(String sql, List<Object[]> batch);

    /**
     * ⚙️ Default implementation.
     */
    final class Default implements JdbcClient {
        private final JdbcTemplate template;

        public Default(JdbcTemplate template) {
            this.template = template;
        }

        @Override
        public int update(String sql, Object... params) {
            return template.update(sql, params);
        }

        @Override
        public int insert(String sql, KeyHolder keys, Object... params) {
            return template.updateAndReturnKeys(sql, keys, params);
        }

        @Override
        public <T> List<T> query(String sql, RowMapper<T> mapper, Object... params) {
            return template.query(sql, mapper, params);
        }

        @Override
        public <T> Optional<T> one(String sql, RowMapper<T> mapper, Object... params) {
            var list = query(sql, mapper, params);
            return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
        }

        @Override
        public <T> T required(String sql, RowMapper<T> mapper, Object... params) {
            var list = query(sql, mapper, params);
            if (list.isEmpty()) {
                throw new org.jmouse.jdbc.errors.EmptyResultException("No rows");
            }
            if (list.size() > 1) {
                throw new org.jmouse.jdbc.errors.NonUniqueResultException("More than one row");
            }
            return list.get(0);
        }

        @Override
        public int[] batch(String sql, List<Object[]> batch) {
            return template.batch(sql, batch);
        }
    }

}
