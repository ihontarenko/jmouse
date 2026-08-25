package org.jmouse.query.spring.builder;

import org.jmouse.query.spring.builder.QuerySubject.SavedQueryHolder;
import org.jmouse.query.sql.SourceLoader;
import org.jmouse.query.translate.JmqTranslator;
import org.jmouse.query.store.QueryOwner;
import org.jmouse.query.store.SavedQueries;
import org.jmouse.query.store.SavedQuery;
import org.jmouse.query.store.SavedQueryCriteria;
import org.jmouse.query.store.SavedQueryDraft;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * The questions somebody kept — listed, saved, renamed and discarded, for every product at once.
 *
 * <h2>⚠️ One controller because there is one table, and one table because a view names a SOURCE</h2>
 *
 * <p>A saved view says {@code issues} or {@code inventory}, and what that reaches is resolved by
 * whichever product's engine runs it. So two installations differ in what their sources mean and never
 * in the shape of these rows — which is why a saved-view controller per product was two implementations
 * of one thing, with the half of the features each happened to need.</p>
 *
 * <h2>⚠️ Who a view belongs to is the SUBJECT's answer, never this class's</h2>
 *
 * <p>{@link QuerySubject#holder} decides the owner and the author, because only a product knows whether
 * a view hangs off a member, a workspace or a board. A subject that answers empty keeps no views, and
 * these endpoints say so rather than filing them somewhere plausible: a default owner invented here
 * would put one caller's saved views where another caller can see them, which is invisible until
 * somebody asks about a view they never wrote.</p>
 *
 * <h2>⚠️ Authorisation is the subject's too, and it runs before every answer</h2>
 *
 * <p>Same gate as the schema and the translation — {@link QuerySubject#authorize}. Listing what a person
 * saved discloses what they are watching, so it is not a weaker question than reading the vocabulary.</p>
 *
 * <h2>⚠️ Transactional at the class, because the store PERSISTS</h2>
 *
 * <p>A shared {@code EntityManager} outside a transaction reads perfectly well and refuses to write —
 * <em>No EntityManager with actual transaction available for current thread</em>, thrown from inside the
 * library rather than from the product, which is the least helpful place for it to surface. The reads are
 * marked read-only individually.</p>
 *
 * <p>⚠️ And the entity is the LIBRARY's, so a product adopting the store has to name
 * {@code org.jmouse.query.store.jpa} in its {@code @EntityScan}. Without it Hibernate answers
 * <em>Could not resolve root entity 'SavedQueryRow'</em> at the first call rather than at startup — the
 * same trap every other {@code jmouse-*-jpa} library here carries.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
@RestController
@Transactional
@RequestMapping(QueryRoutes.PREFIX)
public class SavedQueryController {

    /**
     * ⚠️ Stateless, so one instance is right — and it is constructed rather than injected because a
     * translator into the language itself has nothing to configure: no vendor, no dialect, no source.
     * Making it a bean would invite a product to replace it, and a product with its own idea of how jMQ
     * is spelled is the one thing this seam exists to prevent.
     */
    private static final JmqTranslator JMQ = new JmqTranslator();

    private final QuerySubjects  subjects;
    private final SavedQueries   store;
    private final QueryCallers   callers;
    private final QueryBuilders  builders;

    public SavedQueryController(QuerySubjects subjects, SavedQueries store, QueryCallers callers,
                                QueryBuilders builders) {
        this.subjects = subjects;
        this.store = store;
        this.callers = callers;
        this.builders = builders;
    }

    @GetMapping("/{subject}/views")
    @Transactional(readOnly = true)
    public List<View> list(@PathVariable String subject,
                           @RequestParam Map<String, String> parameters) {
        Held held = held(subject, parameters);

        List<View> shelf = new ArrayList<>();

        // ⚠️ The INSTALLATION's own first — what a fresh workspace has before anybody saves anything.
        // A preset is not a third kind of thing beside a saved view: it is a saved view whose owner is
        // the installation, which is what the store's '*' sentinel exists for. Seeding them as rows is
        // what lets somebody rename one, and what lets the server say which of them still parse — neither
        // of which a list compiled into a frontend bundle can ever offer.
        //
        // ⚠️ And offered ONLY where this listing can answer them. An installation's ready-made question is
        // written against a vocabulary it hopes exists — *running low* means a quantity, and a workspace's
        // bug report has none — so the shelf is narrowed by the subject's own schema rather than by the
        // subject's NAME, which is all the store knows. Dropped silently, because somebody who never had
        // a quantity never wondered where *running low* went.
        gather(shelf, QueryOwner.installation(), held, builders.answerable(held.request()));

        // ⚠️ A person's own view is NOT judged the same way, and the asymmetry is the point. It is already
        // filed against this listing rather than merely against the subject, so it is here because it
        // belongs here — and hiding one whose field somebody has since renamed would take away the only
        // screen it can be repaired from. It refuses when applied, in words, which is the honest failure.
        gather(shelf, held.holder().owner(), held, filter -> true);

        return shelf;
    }

    /**
     * Every listing's kept views, in one request.
     *
     * <h2>⚠️ Why this exists at all</h2>
     *
     * <p>A product registers one subject per <em>thing being listed</em>, so a workspace with forty-four
     * component types is forty-four {@code entries} subjects differing only in their parameters. The
     * manager screen drew a row each and asked twice per row — eighty-eight requests to paint a sidebar.
     * Ivan, 2026-08-25: <em>«багато таких запитів … можливо є сенс створити батч кол?»</em>, and then:
     * <em>«краще зробити батч»</em>.</p>
     *
     * <h2>⚠️ One refusal does not fail the batch</h2>
     *
     * <p>Each element is authorized exactly as the single-subject route authorizes it — this calls the
     * same {@link #held} — and a subject the caller may not read comes back {@code refused} rather than
     * taking the whole answer down with it. A batch that failed as a unit would let one unreadable
     * listing blank a screen that was showing forty-three readable ones.</p>
     *
     * <p>⚠️ <strong>POST, though it reads nothing but.</strong> The question is a list of subjects with
     * their parameters, and that does not fit a query string at forty-four of them.</p>
     */
    @PostMapping("/views/batch")
    @Transactional(readOnly = true)
    public List<Kept> listMany(@RequestBody List<Asked> asked) {
        List<Kept> answers = new ArrayList<>();

        for (Asked one : asked) {
            Map<String, String> parameters = one.parameters() == null ? Map.of() : one.parameters();

            try {
                answers.add(new Kept(one.subject(), parameters, list(one.subject(), parameters), false));
            } catch (RuntimeException refusal) {
                answers.add(new Kept(one.subject(), parameters, List.of(), true));
            }
        }

        return answers;
    }

    /** One listing named in a batch — the subject, and whatever tells two of that name apart. */
    public record Asked(String subject, Map<String, String> parameters) {
    }

    /**
     * ⚠️ The parameters are echoed back, and a client needs them: they are the only thing that tells two
     * subjects of the same name apart, so an answer identified by name alone could not be matched to the
     * row that asked for it.
     */
    public record Kept(String subject, Map<String, String> parameters, List<View> views, boolean refused) {
    }

    private void gather(List<View> shelf, QueryOwner owner, Held held, Predicate<String> offered) {
        store.list(SavedQueryCriteria.ownedBy(owner)
                           .on(held.subject().name())
                           .seenBy(held.holder().author()))
                .stream()
                .filter(kept -> offered.test(kept.getBody()))
                .forEach(kept -> shelf.add(View.of(kept, held.holder().author())));
    }

    /**
     * What this listing IS, written as the declaration nobody typed.
     *
     * <p>⚠️ Read-only and derived — there is nothing here to edit. It exists so a person can see the
     * shape their queries run against rather than infer it from what the builder happens to offer, and so
     * a mapping that has quietly drifted from what somebody remembers is visible instead of surprising.</p>
     */
    @GetMapping("/{subject}/projection")
    @Transactional(readOnly = true)
    public Projection projection(@PathVariable String subject,
                                 @RequestParam Map<String, String> parameters) {
        QuerySubject named  = subjects.named(subject);
        QueryRequest asked  = new QueryRequest(subject, callers.current(), parameters);

        named.authorize(asked);

        return named.source(asked)
                .map(SourceLoader::declare)
                .map(declared -> new Projection(named.name(),
                                                JMQ.translate(declared.toStructure()),
                                                JMQ.translate(declared.toMapping())))
                .orElseGet(() -> new Projection(named.name(), null, null));
    }

    /** ⚠️ Nulls mean this subject declined to show it — not that it has none. See QuerySubject.source. */
    public record Projection(String subject, String structure, String mapping) {
    }

    @PostMapping("/{subject}/views")
    public View save(@PathVariable String subject,
                     @RequestParam Map<String, String> parameters,
                     @RequestBody Draft draft) {
        Held held = held(subject, parameters);

        return View.of(
                store.save(UUID.randomUUID().toString(), draft.into(held)),
                held.holder().author());
    }

    @PutMapping("/{subject}/views/{identifier}")
    public View update(@PathVariable String subject,
                       @PathVariable String identifier,
                       @RequestParam Map<String, String> parameters,
                       @RequestBody Draft draft) {
        Held held = held(subject, parameters);

        mine(identifier, held);

        return View.of(store.update(identifier, draft.into(held)), held.holder().author());
    }

    @DeleteMapping("/{subject}/views/{identifier}")
    public ResponseEntity<Void> remove(@PathVariable String subject,
                                       @PathVariable String identifier,
                                       @RequestParam Map<String, String> parameters) {
        Held held = held(subject, parameters);

        mine(identifier, held);
        store.remove(identifier);

        return ResponseEntity.noContent().build();
    }

    /**
     * ⚠️ Refused where the caller is not the author, HERE rather than in the browser. The screen hides
     * the controls on a view it may not change, and hiding a control is a courtesy — this is the rule.
     */
    private void mine(String identifier, Held held) {
        SavedQuery kept = store.require(identifier);

        if (!kept.getAuthor().equals(held.holder().author())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "This view belongs to somebody else — copy it under a name of your own instead.");
        }
    }

    private Held held(String name, Map<String, String> parameters) {
        QuerySubject subject = subjects.named(name);
        QueryRequest request = new QueryRequest(name, callers.current(), parameters);

        subject.authorize(request);

        SavedQueryHolder holder = subject.holder(request).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_IMPLEMENTED,
                "'%s' does not keep saved views in this product.".formatted(name)));

        return new Held(subject, request, holder);
    }

    /**
     * ⚠️ The REQUEST is carried rather than rebuilt. What narrows a listing lives in its parameters —
     * which form, which project — so a second {@code QueryRequest} assembled further down would be built
     * from whatever that call site happened to hold, which is how a shelf ends up judged against a
     * different listing than the one it was gathered for.
     */
    private record Held(QuerySubject subject, QueryRequest request, SavedQueryHolder holder) {
    }

    /** What a screen sends when a view is kept or renamed. */
    public record Draft(String name, String description, String filter, String order, Boolean shared) {

        private SavedQueryDraft into(Held held) {
            SavedQueryDraft draft = SavedQueryDraft
                    .on(held.subject().name(), held.holder().owner())
                    .named(name)
                    .writing(filter == null ? "" : filter)
                    .by(held.holder().author());

            if (description != null) {
                draft = draft.describedAs(description);
            }

            return Boolean.TRUE.equals(shared) ? draft.visibleToEveryone() : draft;
        }
    }

    /**
     * ⚠️ {@code editable} is answered by the SERVER. A screen working out who may edit is a second
     * implementation of a permission, and two implementations disagree the day one of them is changed.
     */
    public record View(String id, String name, String description, String filter, String order,
                       boolean shared, boolean editable) {

        private static View of(SavedQuery kept, String caller) {
            return new View(kept.getIdentifier(), kept.getName(), kept.getDescription(),
                    kept.getBody(), null, kept.isShared(), kept.getAuthor().equals(caller));
        }
    }
}
