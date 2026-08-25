package org.jmouse.query.spring.builder;

import org.jmouse.query.spring.builder.QuerySubject.SavedQueryHolder;
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
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

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
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
@RestController
@RequestMapping(QueryRoutes.PREFIX)
public class SavedQueryController {

    private final QuerySubjects  subjects;
    private final SavedQueries   store;
    private final QueryCallers   callers;

    public SavedQueryController(QuerySubjects subjects, SavedQueries store, QueryCallers callers) {
        this.subjects = subjects;
        this.store = store;
        this.callers = callers;
    }

    @GetMapping("/{subject}/views")
    public List<View> list(@PathVariable String subject,
                           @RequestParam Map<String, String> parameters) {
        Held held = held(subject, parameters);

        return store.list(SavedQueryCriteria.ownedBy(held.holder().owner())
                                  .on(held.subject().name())
                                  .seenBy(held.holder().author()))
                .stream()
                .map(kept -> View.of(kept, held.holder().author()))
                .toList();
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

        return new Held(subject, holder);
    }

    private record Held(QuerySubject subject, SavedQueryHolder holder) {
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
