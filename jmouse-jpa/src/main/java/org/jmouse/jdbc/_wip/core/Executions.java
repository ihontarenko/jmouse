package org.jmouse.jdbc._wip.core;

import org.jmouse.jdbc._wip.core.mapping.RowMapper;
import org.jmouse.jdbc.errors.EmptyResultException;
import org.jmouse.jdbc.errors.NonUniqueResultException;
import org.jmouse.jdbc.spi.SQLExceptionTranslator;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * ⚙️ Tiny helpers to execute statements with explicit lifecycle control.
 */
public final class Executions {
    private Executions() {
    }

    public static int update(Connection connection, Sql sql, ParameterBinder binder, SQLExceptionTranslator translator) {
        try (PreparedStatement preparedStatement = sql.prepare(connection)) {
            binder.bind(preparedStatement);
            return preparedStatement.executeUpdate();
        } catch (SQLException exception) {
            throw translator.translate("UPDATE", sql.text(), exception);
        }
    }

    public static <T> T queryOne(Connection connection, Sql sql, ParameterBinder binder, RowMapper<T> mapper, SQLExceptionTranslator tr) {
        try (PreparedStatement preparedStatement = sql.prepare(connection)) {
            binder.bind(preparedStatement);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new EmptyResultException("NO ROWS FOR: %s".formatted(sql));
                }

                T value = mapper.map(resultSet, 0);

                if (resultSet.next()) {
                    throw new NonUniqueResultException("MULTIPLE ROWS FOR: %s".formatted(sql));
                }

                return value;
            }
        } catch (SQLException exception) {
            throw tr.translate("QUERY_ONE", sql.text(), exception);
        }
    }
}
