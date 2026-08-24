package org.jmouse.query.schema;

import java.util.Collection;
import java.util.Optional;

/**
 * What may be filtered here — the seam every product implements.
 *
 * <p>Without one, neither the semantic check nor a compiler can do anything: an expression naming
 * {@code entry[component_name]} is just three tokens until something says whether that attribute exists,
 * what it holds, and how to reach it.</p>
 *
 * <h2>⚠️ A product describes itself; the library never guesses</h2>
 *
 * <p>The temptation is to have the library infer a schema — from an entity, an annotation, a table. It
 * is the wrong shape, because the two products this was designed for differ in <em>kind</em>: one keeps
 * every value as a row in a bag with a name, the other has real columns. Any inference clever enough to
 * cover both would be a second product-specific implementation living in the library, where nobody would
 * think to look for it.</p>
 *
 * <p>An implementation is usually thin. Where rows are form entries, the <strong>form already describes
 * them</strong> — every field's name, type and options — and the schema is a view over that plus a short
 * declaration for the few things that are not fields. That is the whole reason this interface is two
 * methods.</p>
 *
 * <h2>The two methods answer two different questions</h2>
 *
 * <ul>
 *   <li>{@link #attribute(String)} — <em>may this be used?</em> Asked by the checker, once per name in
 *       an expression.</li>
 *   <li>{@link #attributes()} — <em>what is on offer?</em> Asked by a builder drawing controls, and by
 *       a refusal that wants to suggest something close.</li>
 * </ul>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public interface QuerySchema {

    /**
     * The attribute written under this name, if there is one.
     *
     * @param name the name as it appears in a query
     * @return the attribute, or empty when nothing here is called that
     */
    Optional<QueryAttribute> attribute(String name);

    /**
     * Everything that may be filtered, sorted or returned here.
     *
     * @return the attributes on offer, in whatever order suits a person reading a list of them
     */
    Collection<QueryAttribute> attributes();

    /**
     * A schema that admits nothing.
     *
     * <p>⚠️ Useful in a test and honest in production: a target nobody has described yet refuses every
     * expression by name, rather than accepting whatever it is handed and failing later somewhere with a
     * worse message.</p>
     *
     * @return a schema with no attributes
     */
    static QuerySchema empty() {
        return new QuerySchema() {

            @Override
            public Optional<QueryAttribute> attribute(String name) {
                return Optional.empty();
            }

            @Override
            public Collection<QueryAttribute> attributes() {
                return java.util.List.of();
            }
        };
    }
}
