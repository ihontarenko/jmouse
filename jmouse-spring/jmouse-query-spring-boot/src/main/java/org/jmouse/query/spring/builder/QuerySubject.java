package org.jmouse.query.spring.builder;

import org.jmouse.query.compose.ConverterPolicy;
import org.jmouse.query.schema.QueryAttribute;
import org.jmouse.query.schema.QuerySchema;

import java.util.List;
import java.util.Map;

/**
 * One thing a product lets people write queries about — its whole contribution to the shared builder.
 *
 * <h2>⚠️ A product declares a subject; it does not write a screen, a controller or a DTO</h2>
 *
 * <p>Every product with a filterable listing was about to grow the same four things: an endpoint saying
 * what may be named, an endpoint reading a written query, a translation from builder rows to text, and
 * the reverse. All four are the same code operating on a different schema — so the schema is what a
 * product supplies and the rest belongs here.</p>
 *
 * <p>Bean per subject. Registering one is the whole cost of a new filterable listing, in any product.</p>
 *
 * <h2>⚠️ What is deliberately NOT here</h2>
 *
 * <p>Running the query. This seam is about <em>composing</em> and <em>reading</em> one — the half that is
 * identical everywhere. Executing it needs a scope built from the session, a source, paging and a way to
 * load whatever matched, none of which is the same in two products and none of which should be smuggled
 * through a shared controller.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public interface QuerySubject {

    /**
     * The segment that names it in a URL, and the key a screen sends.
     *
     * <p>⚠️ Lower-case and plural reads best — {@code entries}, {@code assets}, {@code issues} — because
     * it is what a listing holds.</p>
     */
    String name();

    /**
     * ⚠️ Refuses this caller, or returns — called before ANY answer about this subject.
     *
     * <p><strong>The gate is per subject, not per controller, and it has to be.</strong> One address
     * family serves every listing in the product, and two listings do not need the same permission:
     * somebody who may read entries has no business reading which fields describe the equipment. Gating
     * the controller would either publish one subject to everybody who can reach another, or force every
     * subject behind the narrowest permission any of them needs.</p>
     *
     * <p>⚠️ And a vocabulary is not public data. Attribute labels are a workspace's field names, and the
     * options a select offers are its catalogue. The schema endpoint discloses both.</p>
     *
     * <p>Implemented with the product's own engine, in the product's own words — this module holds no
     * notion of a permission, a workspace or a module, and gains nothing by growing one.</p>
     */
    default void authorize(QueryRequest request) {
    }

    /**
     * What a query written against this subject may name.
     *
     * @param request what arrived — parameters narrowing the vocabulary, and who is asking
     */
    QuerySchema schema(QueryRequest request);

    /**
     * ⚠️ Which converter an ordered comparison needs, stated once and used <strong>twice</strong>: it
     * fills the schema a screen is shown and it is placed into the query that screen composes. Two
     * answers from one object cannot disagree; two implementations always eventually do.
     */
    default ConverterPolicy converters() {
        return ConverterPolicy.NONE;
    }

    /**
     * The values a query here may name without having written them — {@code currentMember}, a tenant.
     *
     * <p>⚠️ Supplied to the <strong>checker</strong> as well as to whatever runs the query. A name the
     * checker has not been told about is refused as unknown, so a builder validating on every keystroke
     * would call a perfectly good query broken.</p>
     */
    default Map<String, Object> values(QueryRequest request) {
        return Map.of();
    }

    /**
     * How the attributes are shown to a person — the half the schema deliberately does not hold.
     *
     * <p>The schema says what a query may <em>write</em>. A word somebody reads and the choices a select
     * offers are a different thing that only a screen needs, and only the product knows.</p>
     *
     * <p>⚠️ <strong>All of them in one call, keyed by attribute name.</strong> Asked per attribute, a
     * product resolving them from an entity would load it once per field — which is a listing of thirty
     * fields becoming thirty queries, and the kind of cost nobody sees because each one is fast.</p>
     *
     * <p>An attribute this does not mention falls back to its own name and no options, so a product with
     * nothing to add implements nothing.</p>
     */
    default Map<String, Presentation> presentations(QuerySchema schema, QueryRequest request) {
        return Map.of();
    }

    /**
     * @param label   what a person calls it
     * @param options the choices a closed set offers, so the builder draws a select rather than a box —
     *                ⚠️ empty means <em>anything</em>, never <em>nothing</em>
     */
    record Presentation(String label, List<String> options) {
    }
}
