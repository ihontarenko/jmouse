package org.jmouse.query.store;

import org.jmouse.el.node.Expression;
import org.jmouse.query.el.QueryLanguage;
import org.jmouse.query.el.node.QueryDocumentNode;
import org.jmouse.query.el.node.ViewNode;
import org.jmouse.query.schema.QueryCheckException;
import org.jmouse.query.schema.QueryChecker;
import org.jmouse.query.schema.QuerySchema;
import org.jmouse.query.store.exception.QueryStoreException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 📖 The saved queries of an installation — read, written, and never accepted broken.
 *
 * <h2>⚠️ Checking is here, in front of every store, and that is the whole point of the class</h2>
 *
 * <p>A saved query is written once and read for years, by people who did not write it, through screens
 * that offer no way to fix it. So the moment to refuse nonsense is the moment somebody could still say
 * what they meant — while they are looking at the text box. Every path in reaches the same three
 * questions:</p>
 *
 * <ol>
 *   <li><strong>Does anything describe this source?</strong> A row naming a source no product describes
 *       can never be run, checked or repaired.</li>
 *   <li><strong>Does it parse?</strong> As a document or as one condition, whichever it is.</li>
 *   <li><strong>Does it make sense against that source?</strong> Every attribute it names, and every
 *       ordered comparison it makes — the check that stops {@code "900" &gt; "1000"} being saved as
 *       true.</li>
 * </ol>
 *
 * <p>Putting this in each store would have it drift between backends, and a query saved through one
 * would then be refused by another.</p>
 *
 * <h2>⚠️ One library, one shape, per-product meaning</h2>
 *
 * <p>Nothing here knows a table, a column or a product. Two installations differ only in the catalogue
 * they are built with — one describing issues and boards, another describing inventory and entries —
 * and the rows, the refusals and the screens above them are the same in both.</p>
 *
 * <pre>{@code
 * QueryLibrary library = new QueryLibrary(
 *         store, engine.language(),
 *         source -> engine.source(source).map(QuerySource::schema));
 *
 * SavedQuery saved = library.save(SavedQueryDraft
 *         .on("issues", QueryOwner.of("BOARD", board.getId()))
 *         .named("Blocked, mine")
 *         .writing("issue.status == 'blocked' and issue.assignee == currentUser()")
 *         .by(member.getId())
 *         .visibleToEveryone());
 * }</pre>
 *
 * <p>Transaction demarcation is the caller's.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class QueryLibrary {

    private final SavedQueries store;
    private final QueryLanguage language;
    private final SchemaCatalog catalog;

    /**
     * 🏗️ A library over one store, reading one language, against one catalogue of sources.
     *
     * @param store    where the rows are kept
     * @param language the jMQ reader — the product's own, so that its functions are in scope here too
     * @param catalog  what each source name lets a query say
     */
    public QueryLibrary(SavedQueries store, QueryLanguage language, SchemaCatalog catalog) {
        this.store    = store;
        this.language = language;
        this.catalog  = catalog;
    }

    /**
     * 💾 Keep a query, under an identifier this library mints.
     *
     * @param draft what to keep
     * @return the saved query
     */
    public SavedQuery save(SavedQueryDraft draft) {
        return saveAs(UUID.randomUUID().toString(), draft);
    }

    /**
     * 💾 Keep a query under an identifier the product minted.
     *
     * <p>⚠️ Worth having rather than tidy: a board that names its filter has to be able to write both
     * rows in one transaction, and it cannot do that if the identifier only exists after the save.</p>
     *
     * @param identifier the identifier to keep it under
     * @param draft      what to keep
     * @return the saved query
     */
    public SavedQuery saveAs(String identifier, SavedQueryDraft draft) {
        check(draft);

        return store.save(identifier, draft);
    }

    /**
     * ✏️ Replace what a saved query says.
     *
     * @param identifier which query
     * @param draft      what it should now say
     * @return the updated query
     */
    public SavedQuery update(String identifier, SavedQueryDraft draft) {
        check(draft);

        return store.update(identifier, draft);
    }

    /**
     * 🔎 One query, if it is there.
     *
     * @param identifier which query
     * @return the query, or empty
     */
    public Optional<SavedQuery> find(String identifier) {
        return store.find(identifier);
    }

    /**
     * 🔎 One query, or a refusal naming it.
     *
     * @param identifier which query
     * @return the query
     */
    public SavedQuery require(String identifier) {
        return store.require(identifier);
    }

    /**
     * 📋 Everything answering these criteria.
     *
     * @param criteria what to list
     * @return the queries
     */
    public List<SavedQuery> list(SavedQueryCriteria criteria) {
        return store.list(criteria);
    }

    /**
     * 🗑️ Forget one.
     *
     * @param identifier which query
     * @return whether there had been one
     */
    public boolean remove(String identifier) {
        return store.remove(identifier);
    }

    /**
     * 📐 Which shape a saved query is written in.
     *
     * @param query the saved query
     * @return whether it is one condition or a whole declaration
     */
    public QueryForm formOf(SavedQuery query) {
        return QueryForm.of(language, query.getBody());
    }

    /**
     * 🧾 The same query in the language's own spelling.
     *
     * <p>⚠️ <strong>Offered, never applied on save.</strong> Rewriting is a rendering of the query
     * rather than an edit of the text, and comments do not survive it — so a product that normalises
     * what somebody typed has to be the one that decided to.</p>
     *
     * @param query the saved query
     * @return the canonical text, or the body unchanged where it is one condition
     */
    public String canonical(SavedQuery query) {
        return formOf(query).isDocument() ? language.rewrite(query.getBody()) : query.getBody();
    }

    /**
     * ✅ Everything that must be true before a query may be kept.
     *
     * <p>Public because the same three questions answer a preview: a screen can ask them while somebody
     * types, and show the identical sentence it would have shown on save.</p>
     *
     * @param draft what would be kept
     * @throws QueryStoreException when a field is missing, too long, or names an undescribed source
     * @throws QueryCheckException when the jMQ does not make sense against that source
     */
    public void check(SavedQueryDraft draft) {
        requirePresent(draft.name(), "a name");
        requirePresent(draft.body(), "something to query");

        requireWithin("name", draft.name(), SavedQueryDraft.MAXIMUM_NAME_LENGTH);
        requireWithin("description", draft.description(), SavedQueryDraft.MAXIMUM_DESCRIPTION_LENGTH);
        requireWithin("query", draft.body(), SavedQueryDraft.MAXIMUM_BODY_LENGTH);
        requireWithin("owner type", draft.owner().type(), QueryOwner.MAXIMUM_LENGTH);
        requireWithin("owner identifier", draft.owner().identifier(), QueryOwner.MAXIMUM_LENGTH);

        QuerySchema schema = catalog.schema(draft.source())
                .orElseThrow(() -> QueryStoreException.unknownSource(draft.source()));

        QueryChecker checker = new QueryChecker(schema);

        if (language.isDocument(draft.body())) {
            QueryDocumentNode document = language.document(draft.body());

            document.getViews().forEach(view -> requireSameTarget(view, draft.source()));

            checker.check(document);
        } else {
            Expression condition = language.expression(draft.body());

            // ⚠️ checkCondition, not check: a body in this shape is a row filter — the same thing a
            // `where` clause holds — so it is refused an aggregate for the same reason. Saving is the
            // last moment anybody can be told; on read it is a row that fails for every viewer.
            checker.checkCondition(condition);
        }
    }

    /**
     * ⚠️ A view inside a saved query may not name a target other than the row's source.
     *
     * <p>They are two statements of one thing, and when they disagree nothing raises: the row's source
     * decides which schema the query was checked against, while the view's target decides what it
     * actually runs against. A query checked against issues and run against inventory does not fail —
     * it answers, and the answer is about the wrong data.</p>
     */
    private void requireSameTarget(ViewNode view, String source) {
        String target = view.getTarget();

        if (target != null && !target.equals(source)) {
            throw new QueryStoreException(
                    ("this query is kept against '%s' but its view reads 'on %s' — it would be checked "
                     + "against one and run against the other. Make them the same.")
                            .formatted(source, target));
        }
    }

    private void requirePresent(String value, String what) {
        if (value == null || value.isBlank()) {
            throw QueryStoreException.missing(what);
        }
    }

    private void requireWithin(String field, String value, int maximum) {
        if (value != null && value.length() > maximum) {
            throw QueryStoreException.tooLong(field, value.length(), maximum);
        }
    }
}
