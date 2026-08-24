package org.jmouse.query.store;

import org.jmouse.query.schema.QuerySchema;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 📚 What each described source lets a query say.
 *
 * <h2>⚠️ The one seam that makes a single store serve every product</h2>
 *
 * <p>A saved row carries the <em>name</em> of a source and nothing else — {@code issues},
 * {@code inventory}. What that name reaches is the product's business: its tables, its columns, its
 * bags, its joins. This is how the store asks the product what a name means, without acquiring an
 * opinion about any of it.</p>
 *
 * <p>A product that already builds an engine over declared sources satisfies this with a lambda, and
 * nothing new has to be maintained in step:</p>
 *
 * <pre>{@code
 * SchemaCatalog catalog = source -> engine.source(source).map(QuerySource::schema);
 * }</pre>
 *
 * <p>⚠️ There is deliberately no permissive catalogue that admits everything. Accepting a query nobody
 * can describe is accepting a row that will be discovered, unrunnable, by whoever opens it next — and
 * the person who could have said what they meant is long gone by then.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
@FunctionalInterface
public interface SchemaCatalog {

    /**
     * What this source lets a query say, if anything describes it.
     *
     * @param source the source name as a saved query carries it
     * @return the schema, or empty when nothing here describes that name
     */
    Optional<QuerySchema> schema(String source);

    /**
     * 📚 A catalogue over schemas already in hand.
     *
     * @param schemas source name to what it offers
     * @return the catalogue
     */
    static SchemaCatalog of(Map<String, QuerySchema> schemas) {
        Map<String, QuerySchema> copy = new LinkedHashMap<>(schemas);

        return source -> Optional.ofNullable(copy.get(source));
    }
}
