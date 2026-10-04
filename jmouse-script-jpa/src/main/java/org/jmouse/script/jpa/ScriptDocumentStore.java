package org.jmouse.script.jpa;

import jakarta.persistence.EntityManager;
import org.jmouse.script.ScriptBinding;
import org.jmouse.script.ScriptDocumentBinder;
import org.jmouse.script.ScriptTemplate;
import org.jmouse.script.spi.ScriptStage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

/**
 * Writing, assigning and removing the rules an installation keeps.
 *
 * <h2>⚠️ IT ANSWERS ENTITIES, NOT VIEWS — AND THAT IS WHERE THE LIBRARY STOPS</h2>
 *
 * <p>Where this came from, the same class also built the screen's DTOs and threw the product's own
 * "not found" and "invariant violated" exceptions. Both are a product's, not a mechanism's: a DTO is an
 * API shape somebody versions, and an error model is how a product has decided to say no.</p>
 *
 * <p>So the boundary runs <b>through</b> that class rather than around it. The operations are here; the
 * mapping and the refusals stay in whatever product owns the screen. A product's own service becomes
 * thin, and two products no longer have to agree on an exception type to share a store.</p>
 *
 * <p>⚠️ Which is also why nothing here takes an id. {@link #find(String)} answers an
 * {@link Optional}, and what an empty one means — a 404, a redirect, a refusal with a sentence — is the
 * product's decision.</p>
 *
 * <h2>⚠️ EVERY METHOD WRITES; NONE OF THEM REBUILDS THE INDEX</h2>
 *
 * <p>{@link StoredScriptDocuments#reload()} must run <b>after</b> the writing transaction has settled,
 * not inside it — a rebuild that read its own uncommitted write would index a document the next reader
 * cannot see. Calling it from in here would make that mistake the default, so it is deliberately the
 * caller's last step.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class ScriptDocumentStore {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScriptDocumentStore.class);

    private static final String EVERY_DOCUMENT = """
            SELECT DISTINCT document FROM ScriptDocument document
            LEFT JOIN FETCH document.assignments
            ORDER BY document.sortOrder ASC, document.name ASC
            """;

    /**
     * ⚠️ <strong>{@code IS NULL} written out rather than {@code = :scope} with a null bound.</strong>
     * SQL's null is not equal to anything, itself included, so the parameterised form silently answers
     * nothing for the installation's own rules — which is the majority of them in a single-tenant
     * product, and a listing that comes back empty rather than failing.
     */
    private static final String IN_SCOPE = """
            SELECT DISTINCT document FROM ScriptDocument document
            LEFT JOIN FETCH document.assignments
            WHERE (:scope IS NULL AND document.scope IS NULL) OR document.scope = :scope
            ORDER BY document.sortOrder ASC, document.name ASC
            """;

    private static final String COUNT_BY_NAME = """
            SELECT COUNT(document) FROM ScriptDocument document
            WHERE document.name = :name
              AND ((:scope IS NULL AND document.scope IS NULL) OR document.scope = :scope)
            """;

    private static final String COUNT_IN_SCOPE = """
            SELECT COUNT(document) FROM ScriptDocument document
            WHERE (:scope IS NULL AND document.scope IS NULL) OR document.scope = :scope
            """;

    private static final String NAMED = """
            SELECT document FROM ScriptDocument document
            WHERE document.name = :name
              AND ((:scope IS NULL AND document.scope IS NULL) OR document.scope = :scope)
            """;

    private final EntityManager        entityManager;
    private final ScriptDocumentBinder binder;
    private final ScriptTemplate       template;

    public ScriptDocumentStore(EntityManager entityManager, ScriptDocumentBinder binder,
                               ScriptTemplate template) {
        this.entityManager = entityManager;
        this.binder = binder;
        this.template = template;
    }

    /**
     * Every rule, in the order they run, with their assignments already in hand.
     *
     * <p>⚠️ Fetch-joined: a screen that listed them and then touched each one's assignments would be the
     * classic N+1, on the page somebody opens to see where their rules run.</p>
     *
     * @return the rules
     */
    public List<ScriptDocument> all() {
        return entityManager.createQuery(EVERY_DOCUMENT, ScriptDocument.class).getResultList();
    }

    /**
     * The rules filed under one tenant — or, for {@code null}, the installation's own.
     *
     * <h2>⚠️ THIS IS NOT WHAT A TENANT'S RULES <em>RUN</em> AS, AND THE DIFFERENCE IS DELIBERATE</h2>
     *
     * <p>A dispatch in a tenant also runs the installation's rules — see
     * {@code ScriptDocuments#forStage}. This answers what is <strong>filed there</strong>, which is the
     * question an editor asks: somebody managing a workspace's rules is managing that workspace's, and
     * a list that mixed in rules they cannot edit would be a list of things that do not respond.
     *
     * <p>⚠️ A screen that wants to show *everything that will run here* composes the two and says which
     * is which. It is not this method's job to decide that a reader can tell them apart.
     *
     * @param scope the tenant, or {@code null} for the installation's own
     * @return the rules, in the order they run, with their assignments already in hand
     */
    public List<ScriptDocument> inScope(String scope) {
        return entityManager.createQuery(IN_SCOPE, ScriptDocument.class)
                .setParameter("scope", scope)
                .getResultList();
    }

    /**
     * One rule.
     *
     * @param id its identifier
     * @return the rule, or empty — what that means is the caller's to decide
     */
    public Optional<ScriptDocument> find(String id) {
        return Optional.ofNullable(id == null ? null : entityManager.find(ScriptDocument.class, id));
    }

    /**
     * Creates a rule, already assigned to one moment and already carrying that moment's guard.
     *
     * @param title what somebody typed; the name is derived from it
     * @param stage where it will run
     * @return the rule
     */
    public ScriptDocument create(String title, ScriptStage<?> stage) {
        return create(null, title, stage);
    }

    /**
     * Creates a rule in one tenant, already assigned to one moment and carrying that moment's guard.
     *
     * <p>⚠️ The name is made unique <strong>within the scope</strong> and the sort order counted within
     * it too. Both were installation-wide before scopes: a second tenant naming a rule
     * <em>low-stock</em> would have been given <em>low-stock-2</em> for no reason it could see, and its
     * first rule would have sorted after every other tenant's.
     *
     * @param scope the tenant, or {@code null} for the installation's own
     * @param title what somebody typed; the name is derived from it
     * @param stage where it will run
     * @return the rule
     */
    public ScriptDocument create(String scope, String title, ScriptStage<?> stage) {
        String         name     = uniqueNameFrom(scope, title);
        ScriptDocument document = new ScriptDocument(name, template.forStage(name, stage));

        document.setScope(scope);
        document.setDescription(stage.detail());
        document.setSortOrder((int) countIn(scope));
        document.assignTo(stage.name());
        document.bound();

        entityManager.persist(document);

        LOGGER.info("rule '{}' created at '{}'{}", name, stage.name(),
                    scope == null ? "" : " in " + scope);

        return document;
    }

    /**
     * Rewrites a rule's text and description, and records whether it still binds.
     *
     * <p>⚠️ Checked under the WIDEST ceiling — see {@code ScriptDocumentBinder.check}. This answers
     * whether the text is valid, never whether it would fit inside a particular moment's budget: a rule
     * can only exceed a budget by running, and running it to find out is not something a save may do.</p>
     *
     * @param document    the rule
     * @param source      its new text
     * @param description its new description
     * @return the rule
     */
    public ScriptDocument rewrite(ScriptDocument document, String source, String description) {
        document.setSource(source == null ? "" : source);
        document.setDescription(description);

        ScriptBinding binding = binder.check(document.getName(), document.getSource());

        if (binding.wasAccepted()) {
            document.bound();
        } else {
            document.refused("%s: %s".formatted(binding.stage(), binding.problem()));
        }

        return entityManager.merge(document);
    }

    /**
     * Switches a rule on or off.
     *
     * <p>⚠️ A rule switched off is kept, shown, and still bind-checked. Somebody switching one off is
     * usually finding out whether it was the cause of something.</p>
     *
     * @param document the rule
     * @param enabled  whether it runs
     * @return the rule
     */
    public ScriptDocument setEnabled(ScriptDocument document, boolean enabled) {
        document.setEnabled(enabled);

        return entityManager.merge(document);
    }

    /**
     * Assigns a rule to a moment, or takes it off one.
     *
     * <p>⚠️ The caller resolves the stage first, so a name this build does not declare never reaches a
     * row. An assignment stored against a name nothing declares is a rule that looks placed and never
     * fires.</p>
     *
     * @param document the rule
     * @param stage    the moment
     * @param assigned true to place it, false to take it off
     * @return the rule
     */
    public ScriptDocument assign(ScriptDocument document, ScriptStage<?> stage, boolean assigned) {
        boolean changed = assigned
                ? document.assignTo(stage.name())
                : document.unassignFrom(stage.name());

        if (changed) {
            LOGGER.info("rule '{}' {} '{}'",
                        document.getName(), assigned ? "now runs at" : "no longer runs at", stage.name());
        }

        return entityManager.merge(document);
    }

    /**
     * Deletes a rule.
     *
     * <p>⚠️ Its assignments go with it, by cascade — an assignment has no life of its own.</p>
     *
     * @param document the rule
     */
    public void delete(ScriptDocument document) {
        entityManager.remove(entityManager.contains(document) ? document : entityManager.merge(document));

        LOGGER.info("rule '{}' deleted", document.getName());
    }

    /**
     * ⚠️ A name is unique and it is the only handle a document has, so a second rule called the same
     * thing gets a number rather than an error. Somebody creating two rules from the same screen in a row
     * should not have to invent a name before they have written anything.
     *
     * @param title what somebody typed
     * @return a name nothing else holds
     */
    public String uniqueNameFrom(String title) {
        return uniqueNameFrom(null, title);
    }

    /**
     * A name nothing in this tenant holds.
     *
     * @param scope the tenant, or {@code null} for the installation's own
     * @param title what somebody typed
     * @return a name nothing else in that scope holds
     */
    public String uniqueNameFrom(String scope, String title) {
        String base      = template.nameFrom(title);
        String candidate = base;

        for (int suffix = 2; exists(scope, candidate); suffix++) {
            candidate = "%s-%d".formatted(base, suffix);
        }

        return candidate;
    }

    /**
     * One rule, by the name it is known everywhere by.
     *
     * <h2>⚠️ THE PARTNER OF {@link #find(String)}, AND LEAVING IT OUT WAS AN OVERSIGHT</h2>
     *
     * <p>This class already says the name is the only handle a document has — it is what every
     * assignment, every refusal and every log line quotes. A store that could be asked by row identifier
     * and not by that handle was extracted half-finished, and the product that noticed had to keep a
     * second way of reading the same table in order to say <em>have I seeded this one already</em>.</p>
     *
     * @param name the rule's name
     * @return the rule, or empty
     */
    public Optional<ScriptDocument> findNamed(String name) {
        return findNamed(null, name);
    }

    /**
     * One rule in one tenant, by the name it is known there by.
     *
     * @param scope the tenant, or {@code null} for the installation's own
     * @param name  the rule's name
     * @return the rule, or empty
     */
    public Optional<ScriptDocument> findNamed(String scope, String name) {
        if (name == null) {
            return Optional.empty();
        }

        return entityManager.createQuery(NAMED, ScriptDocument.class)
                .setParameter("name", name)
                .setParameter("scope", scope)
                .getResultStream()
                .findFirst();
    }

    /**
     * Puts a document the caller built into the store.
     *
     * <h2>⚠️ THIS IS FOR SEEDING, WHICH IS NOT {@link #create}</h2>
     *
     * <p>{@link #create} is what an editor does: derive a name from a title, start from the stage's
     * template, place it, switch it on. A product shipping example rules or a rule per configured step
     * has all of that already — the body is a file it wrote, the name is fixed because something else
     * refers to it, and it may well want the document switched <em>off</em>. Forcing that through
     * {@code create} would mean creating a document and then correcting five fields.</p>
     *
     * <p>⚠️ Persist or merge, by what the context already holds. A caller seeding a fresh document and a
     * caller re-storing a detached one both mean "make this be what is in the table", and picking wrong
     * is the {@code detached entity passed to persist} that only appears on the second run.</p>
     *
     * @param document the rule
     * @return the stored rule
     */
    public ScriptDocument store(ScriptDocument document) {
        if (document.getCreatedAt() == null && !entityManager.contains(document)) {
            entityManager.persist(document);

            return document;
        }

        return entityManager.merge(document);
    }

    /**
     * How many rules there are.
     *
     * <p>Public because a product that seeds needs it for the same reason {@link #create} does: a new
     * document goes at the end, and the end is however many there already are.</p>
     *
     * @return the count
     */
    public long count() {
        return entityManager.createQuery("SELECT COUNT(document) FROM ScriptDocument document", Long.class)
                .getSingleResult();
    }

    /**
     * How many rules one tenant has — or the installation's own, for {@code null}.
     *
     * @param scope the tenant, or {@code null}
     * @return the count
     */
    public long countIn(String scope) {
        return entityManager.createQuery(COUNT_IN_SCOPE, Long.class)
                .setParameter("scope", scope)
                .getSingleResult();
    }

    private boolean exists(String scope, String name) {
        return entityManager.createQuery(COUNT_BY_NAME, Long.class)
                .setParameter("name", name)
                .setParameter("scope", scope)
                .getSingleResult() > 0;
    }

}
