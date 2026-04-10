package org.jmouse.jdbc.operation.source;

/**
 * Source of SQL definition for a typed SQL operation.
 *
 * <p>A source may represent either inline SQL text or a classpath resource
 * location that points to external SQL.</p>
 *
 * @author Ivan Hontarenko
 */
public sealed interface SqlSource permits InlineSqlSource, ResourceSqlSource {

    /**
     * Returns source value.
     *
     * <p>For {@link InlineSqlSource} this is raw SQL text.
     * For {@link ResourceSqlSource} this is a classpath resource location.</p>
     *
     * @return source value
     */
    String value();

}