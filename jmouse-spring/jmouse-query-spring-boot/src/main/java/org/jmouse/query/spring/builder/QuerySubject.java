package org.jmouse.query.spring.builder;

import org.jmouse.query.compose.ConverterPolicy;
import org.jmouse.query.schema.QueryAttribute;
import org.jmouse.query.schema.QuerySchema;
import org.jmouse.query.sql.QuerySource;
import org.jmouse.query.store.QueryOwner;
import org.jmouse.query.store.SourceOrigin;

import java.util.List;
import java.util.Map;
import java.util.Optional;

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
     * Whether this listing's declaration is something a person wrote, or something derived.
     *
     * <h2>⚠️ {@code DERIVED} is the default, and that is the safe answer</h2>
     *
     * <p>A subject that has not thought about the question cannot accidentally become writable. Saying
     * {@code AUTHORED} is what turns the guards on — a permission this subject decides, and an allow-list
     * of the tables the installation publishes — because a mapping names tables, and the row-level checks
     * on a listing never ask which table the rows came from.</p>
     *
     * <p>⚠️ An entry listing is derived by construction: what a query may name is the fields somebody put
     * on a form, so there is no document to author and an editable copy would be a second truth that goes
     * stale at the next field.</p>
     */
    default SourceOrigin origin() {
        return SourceOrigin.DERIVED;
    }

    /**
     * Refuses a caller who may not <strong>read</strong> this listing's declaration.
     *
     * <h2>⚠️ It defaults to {@link #authorize}, and that default is a decision somebody has to make</h2>
     *
     * <p>A declaration names <strong>tables and columns</strong> — more than the vocabulary, which is
     * only what a query may say. Left on the same gate as writing a query, every caller who may filter a
     * listing can also read the database schema behind it.</p>
     *
     * <p>For an internal product that is usually fine and is why it is the default. It stopped being an
     * accident the moment this method existed: a product that wants it narrower overrides one method,
     * and a product that is content says so by leaving it alone — either way somebody has looked.</p>
     *
     * <p>⚠️ Deliberately separate from {@link #authorizeSourceWrite}. Reading a mapping and rewriting it
     * are different powers by a wide margin: one discloses a schema, the other chooses which tables the
     * product reads from.</p>
     *
     * @param request what was asked
     */
    default void authorizeSourceRead(QueryRequest request) {
        authorize(request);
    }

    /**
     * Refuses a caller who may not rewrite this listing's declaration.
     *
     * <h2>⚠️ Separate from {@link #authorize}, and far narrower</h2>
     *
     * <p>Reading a listing and rewriting what the listing <em>is</em> are not the same permission and must
     * never collapse into one. The default refuses everybody: a product opts a subject in by overriding
     * this, which is one deliberate act rather than a consequence of having declared
     * {@link SourceOrigin#AUTHORED}.</p>
     *
     * @param request what was asked
     */
    default void authorizeSourceWrite(QueryRequest request) {
        throw new IllegalStateException(
                "'%s' does not let anybody rewrite its declaration".formatted(name()));
    }

    /**
     * Who an authored declaration belongs to — <strong>the installation, by default</strong>.
     *
     * <h2>⚠️ Deliberately NOT {@link #holder}, and the difference is not cosmetic</h2>
     *
     * <p>A saved view is one person's question and is filed against a member or a workspace. A
     * declaration is what the listing <em>is</em>: there is one, everybody's queries run against it, and
     * a per-member one would mean two people asking the same listing the same question and getting
     * answers from different tables.</p>
     *
     * <p>Reusing {@code holder} here would have produced exactly that, silently — the endpoints would
     * have worked, the screen would have shown a declaration, and it would have been a private one.</p>
     *
     * <p>A product that genuinely scopes declarations narrower — per tenant, say — overrides this, and
     * then the narrowing is a decision somebody wrote down.</p>
     */
    default QueryOwner declarationOwner(QueryRequest request) {
        return QueryOwner.installation();
    }

    /**
     * ⚠️ The whole source — shape AND binding — for a screen that shows what this listing really is.
     *
     * <p>{@link #schema} answers what a query may NAME; this answers where those values are. A product
     * that builds its sources in Java has that declaration and simply never writes it down, so handing it
     * over is what lets a person read the mapping their queries run against instead of taking it on
     * trust.</p>
     *
     * <p>⚠️ Empty means <em>not shown</em>, not <em>none</em>. A subject whose source is assembled per
     * request out of something it would rather not disclose is entitled to decline, and the screen says
     * the projection is unavailable rather than drawing an approximation.</p>
     */
    default Optional<QuerySource> source(QueryRequest request) {
        return Optional.empty();
    }

    /**
     * Who a saved view belongs to here, and who is keeping it.
     *
     * <h2>⚠️ The product answers, because only the product knows what a view hangs off</h2>
     *
     * <p>The store keeps an owner as a <strong>pair</strong> — a type and an identifier — rather than an
     * enum, precisely so that one product can hang views off a member, another off a workspace, and a
     * third off a board, without the library being released each time somebody finds a new thing to hang
     * them off. This is where that pair is decided.</p>
     *
     * <p>⚠️ <strong>Returning empty means this subject keeps no views</strong>, and the endpoints answer
     * as much rather than filing them somewhere plausible. A default owner invented here would put one
     * caller's saved views where another caller could see them, which is the one mistake in this area
     * that is invisible until somebody complains about a view they never wrote.</p>
     *
     * @param request what arrived, including who is asking
     * @return the owner and the author, or empty where this subject keeps none
     */
    default Optional<SavedQueryHolder> holder(QueryRequest request) {
        return Optional.empty();
    }

    /**
     * Where a saved view is filed, and by whom.
     *
     * @param owner  what holds it — {@code MEMBER}/{@code id}, {@code WORKSPACE}/{@code id}
     * @param author the product's own identifier for the person keeping it
     */
    record SavedQueryHolder(QueryOwner owner, String author) {
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
