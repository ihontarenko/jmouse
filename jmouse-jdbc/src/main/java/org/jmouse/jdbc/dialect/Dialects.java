package org.jmouse.jdbc.dialect;

public final class Dialects {

    private Dialects() {}

    public static DialectRegistry defaults() {
        return new DialectRegistry()
                .register(new AnsiDialect())
                .register(new PostgresDialect())
                .register(new MySqlDialect())
                .register(new H2Dialect())
                .fallback(new AnsiDialect());
    }
}
