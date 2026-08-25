package org.jmouse.query.spring.source;

import org.jmouse.query.el.node.AttributeNode;
import org.jmouse.query.el.node.FieldNode;
import org.jmouse.query.el.node.MappingNode;
import org.jmouse.query.el.node.SourceNode;
import org.jmouse.query.el.node.StructureNode;
import org.jmouse.query.spring.builder.QueryCallers;
import org.jmouse.query.spring.builder.QueryRoutes;
import org.jmouse.query.spring.builder.QueryRequest;
import org.jmouse.query.spring.builder.QuerySubject;
import org.jmouse.query.spring.builder.QuerySubjects;
import org.jmouse.query.store.AuthoredSource;
import org.jmouse.query.store.AuthoredSources;
import org.jmouse.query.store.QueryOwner;
import org.jmouse.query.store.SourceOrigin;
import org.jmouse.query.translate.Bindings;
import org.jmouse.query.translate.JmqTranslator;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Reading and rewriting what a listing IS.
 *
 * <h2>⚠️ Its own controller, because it is its own permission</h2>
 *
 * <p>Saved queries are questions asked of a source; this is the source. They differ in who may write
 * them, in how many there can be, and in what a bad one costs — one shows the wrong rows, the other
 * reaches the wrong table. Sharing a controller would have meant sharing a gate.</p>
 *
 * <h2>⚠️ Nothing here writes jMQ in the browser, including the builder</h2>
 *
 * <p>The attributes builder sends <strong>rows</strong> and is handed text back, exactly as the query
 * builder does. A browser that assembled a declaration would be a second writer for a language that
 * already has one, and the one it competes with is the one that decides what actually runs.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
@RestController
// ⚠️ The SAME prefix its neighbours carry. Without it the routes mount at the application root, every
// call answers 404, and nothing anywhere says why — the third time this area has produced a silent 404
// from a different cause.
@RequestMapping(QueryRoutes.PREFIX)
@Transactional
public class SourceController {

    private static final JmqTranslator JMQ = new JmqTranslator();

    private final QuerySubjects   subjects;
    private final QueryCallers    callers;
    private final QuerySources    sources;
    private final AuthoredSources stored;
    private final PublishedTables published;

    public SourceController(QuerySubjects subjects, QueryCallers callers, QuerySources sources,
                            AuthoredSources stored, PublishedTables published) {
        this.subjects  = subjects;
        this.callers   = callers;
        this.sources   = sources;
        this.stored    = stored;
        this.published = published;
    }

    /**
     * What the editor opens on, and what it is allowed to do.
     *
     * <p>⚠️ {@code writable} is answered by asking the subject and catching its refusal, rather than by
     * the browser comparing a permission name. A screen that worked out for itself who may edit would be
     * a second implementation of the gate, and the two disagree the day one is changed.</p>
     */
    @GetMapping("/{subject}/source")
    @Transactional(readOnly = true)
    public Declaration declaration(@PathVariable String subject,
                                   @RequestParam Map<String, String> parameters) {
        QuerySubject named = subjects.named(subject);
        QueryRequest asked = new QueryRequest(subject, callers.current(), parameters);

        // ⚠️ The READ gate, which is not the same as the one that lets somebody write a query. A
        // declaration names tables and columns; a vocabulary names only what a query may say.
        named.authorizeSourceRead(asked);

        return sources.draft(named, asked)
                .map(draft -> new Declaration(
                        named.name(), named.origin(), draft.body(), draft.authored(),
                        draft.author(), draft.updatedAt(), writable(named, asked),
                        published.publishesAnything()))
                .orElseGet(() -> new Declaration(
                        named.name(), named.origin(), null, false, null, null, false,
                        published.publishesAnything()));
    }

    /**
     * Rewrites it.
     *
     * <p>⚠️ Three refusals, in this order, and the order is the point: <em>may you</em>, then <em>does it
     * parse</em>, then <em>may it reach those tables</em>. Vetting before authorizing would tell somebody
     * who holds nothing whether a table exists.</p>
     */
    @PutMapping("/{subject}/source")
    public Declaration rewrite(@PathVariable String subject,
                               @RequestParam Map<String, String> parameters,
                               @RequestBody Body body) {
        QuerySubject named = subjects.named(subject);
        QueryRequest asked = new QueryRequest(subject, callers.current(), parameters);

        named.authorizeSourceRead(asked);
        named.authorizeSourceWrite(asked);

        requireAuthored(named);
        sources.vet(body.body());

        AuthoredSource written = stored.save(new AuthoredSource(
                named.name(), named.declarationOwner(asked), body.body(), asked.caller(), Instant.now()));

        return new Declaration(
                named.name(), named.origin(), written.body(), true, written.author(),
                written.updatedAt(), true, published.publishesAnything());
    }

    /**
     * Forgets it, so the listing goes back to the declaration the product ships.
     *
     * <p>⚠️ Which is why this is not the destructive act it looks like — there is always something to
     * fall back to, and a source is never left undefined.</p>
     */
    @DeleteMapping("/{subject}/source")
    public Declaration revert(@PathVariable String subject,
                              @RequestParam Map<String, String> parameters) {
        QuerySubject named = subjects.named(subject);
        QueryRequest asked = new QueryRequest(subject, callers.current(), parameters);

        named.authorizeSourceRead(asked);
        named.authorizeSourceWrite(asked);

        // ⚠️ Deliberately NOT behind `requireAuthored`, unlike a rewrite. If a subject was authored and
        // its product later makes it derived in code, the row is inert but still there — and this is the
        // only way left to clear it. Refusing here would strand a row nobody can reach.
        stored.remove(named.declarationOwner(asked), named.name());

        return declaration(subject, parameters);
    }

    /**
     * Says whether a body would be accepted, without storing it.
     *
     * <p>⚠️ A query rather than a mutation, and a refusal is DATA here rather than an error: half-typed
     * text is the normal state of an editor, and a screen that raised every keystroke as a failure would
     * flash red at somebody who has made no mistake yet.</p>
     */
    @PostMapping("/{subject}/source/validate")
    @Transactional(readOnly = true)
    public Verdict validate(@PathVariable String subject,
                            @RequestParam Map<String, String> parameters,
                            @RequestBody Body body) {
        QuerySubject named = subjects.named(subject);
        QueryRequest asked = new QueryRequest(subject, callers.current(), parameters);

        named.authorizeSourceRead(asked);

        try {
            SourceNode declared = sources.vet(body.body());

            return new Verdict(true, "The declaration reads.",
                               List.copyOf(PublishedTables.named(declared)), rows(declared));
        } catch (RuntimeException exception) {
            return new Verdict(false, exception.getMessage(), List.of(), List.of());
        }
    }

    /**
     * Rows in, jMQ out — the attributes builder's one call.
     *
     * <p>⚠️ It does not store anything. The builder produces text and the person still presses save,
     * which is the same rule the query panel follows: nothing is applied until somebody asks.</p>
     */
    @PostMapping("/{subject}/source/compose")
    @Transactional(readOnly = true)
    public Verdict compose(@PathVariable String subject,
                           @RequestParam Map<String, String> parameters,
                           @RequestBody Composition composition) {
        QuerySubject named = subjects.named(subject);
        QueryRequest asked = new QueryRequest(subject, callers.current(), parameters);

        named.authorizeSourceRead(asked);

        try {
            SourceNode declared = assemble(composition);

            published.require(declared);

            return new Verdict(true, JMQ.translate(declared, Bindings.none()),
                               List.copyOf(PublishedTables.named(declared)), rows(declared));
        } catch (RuntimeException exception) {
            return new Verdict(false, exception.getMessage(), List.of(), List.of());
        }
    }

    /**
     * ⚠️ Built as NODES and handed to the translator, never assembled as text.
     *
     * <p>Every rule about how a name is quoted, how a type is spelled and where a colon goes lives in the
     * nodes' own rendering, which is also what parses back. Text written here would round-trip until it
     * did not.</p>
     */
    private SourceNode assemble(Composition composition) {
        StructureNode structure = new StructureNode();
        MappingNode   mapping   = new MappingNode();

        structure.setName(composition.structure());

        mapping.setStructure(composition.structure());
        mapping.setTable(composition.table());
        mapping.setAlias(composition.alias());
        mapping.setKey(composition.key());

        for (Composition.Attribute attribute : composition.attributes()) {
            FieldNode field = new FieldNode();

            field.setName(attribute.name());
            field.setType(attribute.type() == null ? "unknown" : attribute.type());

            structure.addField(field);

            AttributeNode binding = new AttributeNode();

            binding.setName(attribute.name());
            binding.setSource(attribute.source() == null ? attribute.name() : attribute.source());
            binding.setAccess(attribute.access() == null ? "column" : attribute.access());

            mapping.addAttribute(binding);
        }

        return SourceNode.merge(structure, mapping);
    }

    /** The attribute rows a builder draws — the same shape it sends back. */
    private List<Composition.Attribute> rows(SourceNode declared) {
        return declared.getAttributes().stream()
                .map(attribute -> new Composition.Attribute(
                        attribute.getName(), attribute.getSource(),
                        attribute.getType(), attribute.getAccess()))
                .toList();
    }

    private void requireAuthored(QuerySubject subject) {
        if (subject.origin() != SourceOrigin.AUTHORED) {
            throw new IllegalStateException(
                    ("'%s' is derived — what a query may name here follows from something that already "
                     + "exists, so there is nothing to author").formatted(subject.name()));
        }
    }

    private boolean writable(QuerySubject subject, QueryRequest request) {
        if (subject.origin() != SourceOrigin.AUTHORED || !published.publishesAnything()) {
            return false;
        }

        try {
            subject.authorizeSourceWrite(request);
            return true;
        } catch (RuntimeException refused) {
            return false;
        }
    }

    /** What a listing's declaration is, and what may be done to it. */
    public record Declaration(String subject, SourceOrigin origin, String body, boolean authored,
                              String author, Instant updatedAt, boolean writable,
                              boolean publishesTables) {
    }

    /** The server's answer about a body — ⚠️ `readable: false` is data, not a failure. */
    public record Verdict(boolean readable, String message, List<String> tables,
                          List<Composition.Attribute> attributes) {
    }

    /** A body on its way in. */
    public record Body(String body) {
    }

    /** What the attributes builder sends: a shape and where each of its values lives. */
    public record Composition(String structure, String table, String alias,
                              String key, List<Attribute> attributes) {

        public record Attribute(String name, String source, String type, String access) {
        }
    }
}
