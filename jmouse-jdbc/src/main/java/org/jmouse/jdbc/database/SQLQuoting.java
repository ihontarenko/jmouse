package org.jmouse.jdbc.database;

public interface SQLQuoting {

    String quoteIdentifier(String identifier);

    /**
     * Escape a string literal content (without surrounding quotes).
     */
    String escapeLiteral(String raw);

    static SQLQuoting ansi() {
        return new SQLQuoting() {
            @Override
            public String quoteIdentifier(String identifier) {
                return "\"%s\"".formatted(identifier);
            }

            @Override
            public String escapeLiteral(String raw) {
                return raw == null ? null : raw.replace("'", "''");
            }
        };
    }
}
