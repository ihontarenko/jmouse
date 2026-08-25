package org.jmouse.query.spring.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * What a product tells the bridge — and it is deliberately two things.
 *
 * <pre>
 * jmouse:
 *   query:
 *     sources: classpath:jmq/sources.jmq
 *     dialect: mysql          # omit to read it off the connection
 * </pre>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
@ConfigurationProperties(prefix = "jmouse.query")
public class QueryProperties {

    /**
     * Where the {@code source { }} declarations are — a classpath resource.
     *
     * <p>⚠️ A file rather than a bean by default, because a product's mapping is data: it is read by the
     * editor, printed in a demonstration and diffed in a review. A product that genuinely needs logic in
     * a mapping still declares its own {@code QueryEngine} bean, and this one steps aside.</p>
     */
    private String sources = "classpath:jmq/sources.jmq";

    /**
     * {@code mysql} or {@code postgresql}.
     *
     * <p>⚠️ Empty means <em>ask the connection</em>, which is right far more often than a default would
     * be: a product that runs on both keeps one configuration and the dialect follows the database it is
     * actually pointed at. Guessing here would produce a statement that runs on the wrong engine and
     * fails somewhere far away — the interval syntax differs, and that is not a syntax error, it is a
     * different answer.</p>
     */
    private String dialect = "";

    /**
     * The tables an <strong>authored</strong> declaration may name.
     *
     * <pre>
     * jmouse:
     *   query:
     *     published-tables: [ issues, statuses, issue_types, priorities, resolutions ]
     * </pre>
     *
     * <h2>⚠️ Empty means NOTHING is allowed, and it is the default on purpose</h2>
     *
     * <p>A mapping names tables. The permission checks on a listing are about <em>rows</em> — none of
     * them ask which table the rows came from, because until a mapping could be edited the answer was
     * fixed at compile time. So an unconfigured allow-list that permitted everything would turn
     * forgetting this property into silently switching the guard off, and a guard that is off by default
     * is a guard that is off in production.</p>
     *
     * <p>A product that publishes nothing simply has no authored sources, which is what a product that
     * has not thought about this should have.</p>
     */
    private Set<String> publishedTables = new LinkedHashSet<>();

    public Set<String> getPublishedTables() {
        return publishedTables;
    }

    public void setPublishedTables(Set<String> publishedTables) {
        this.publishedTables = publishedTables;
    }

    public String getSources() {
        return sources;
    }

    public void setSources(String sources) {
        this.sources = sources;
    }

    public String getDialect() {
        return dialect;
    }

    public void setDialect(String dialect) {
        this.dialect = dialect;
    }
}
