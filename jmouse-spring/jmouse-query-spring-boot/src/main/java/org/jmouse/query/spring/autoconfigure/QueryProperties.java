package org.jmouse.query.spring.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

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
