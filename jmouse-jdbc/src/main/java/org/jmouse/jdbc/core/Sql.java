package org.jmouse.jdbc.core;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * 🧱 Immutable SQL statement with optional auto-generated-keys flag.
 */
public final class Sql {

    private final String  text;
    private final boolean returnKeys;

    public Sql(String text) {
        this(text, false);
    }

    public Sql(String text, boolean returnKeys) {
        this.text = text;
        this.returnKeys = returnKeys;
    }

    public String text() {
        return text;
    }

    public boolean returnKeys() {
        return returnKeys;
    }

    public PreparedStatement prepare(Connection connection) throws SQLException {
        return returnKeys ? connection.prepareStatement(
                text, Statement.RETURN_GENERATED_KEYS) : connection.prepareStatement(text);
    }

    @Override
    public String toString() {
        return text;
    }
}
