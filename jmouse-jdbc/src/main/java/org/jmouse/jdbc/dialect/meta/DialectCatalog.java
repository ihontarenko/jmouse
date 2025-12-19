package org.jmouse.jdbc.dialect.meta;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class DialectCatalog {

    public static final String POSTGRE_SQL_NAME = "PostgreSQL";
    public static final String POSTGRES_ID = "postgres";
    public static final String MY_SQL_NAME = "MySQL";
    public static final String MYSQL_ID = "mysql";

    public record Rule(DialectMatcher matcher, String dialectId) {
        public Rule {
            Objects.requireNonNull(matcher, "matcher");
            Objects.requireNonNull(dialectId, "dialectId");
        }
    }

    private final List<Rule> rules = new ArrayList<>();

    public DialectCatalog register(DialectMatcher matcher, String dialectId) {
        rules.add(new Rule(matcher, dialectId));
        return this;
    }

    public Optional<String> resolve(DatabaseInfo info) {
        for (Rule r : rules) {
            if (r.matcher().matches(info)) {
                return Optional.of(r.dialectId());
            }
        }
        return Optional.empty();
    }

    public static DialectCatalog defaults() {
        return new DialectCatalog()
                .register(DialectMatcher.byProductName(POSTGRE_SQL_NAME), POSTGRES_ID)
                .register(DialectMatcher.byProductName(MY_SQL_NAME), MYSQL_ID)
                .register(DialectMatcher.byProductName("MariaDB"), "mysql")
                .register(DialectMatcher.byProductName("H2"), "h2")
                .register(DialectMatcher.byProductName("SQLite"), "sqlite")
                .register(DialectMatcher.byProductName("Microsoft SQL Server"), "mssql")
                .register(DialectMatcher.byProductName("Oracle"), "oracle");
    }
}
