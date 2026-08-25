package org.jmouse.query.spring.source;

import org.jmouse.query.el.QueryLanguage;
import org.jmouse.query.el.node.QueryDocumentNode;
import org.jmouse.query.el.node.SourceNode;
import org.jmouse.query.sql.QuerySource;
import org.jmouse.query.sql.SourceLoader;
import org.jmouse.query.spring.builder.QueryRequest;
import org.jmouse.query.spring.builder.QuerySubject;
import org.jmouse.query.store.AuthoredSource;
import org.jmouse.query.store.AuthoredSources;
import org.jmouse.query.store.QueryOwner;
import org.jmouse.query.store.SourceOrigin;
import org.jmouse.query.translate.Bindings;
import org.jmouse.query.translate.JmqTranslator;

import java.time.Instant;
import java.util.Optional;

/**
 * Which declaration a subject actually runs against.
 *
 * <h2>⚠️ ONE answer, and every caller has to ask this</h2>
 *
 * <p>The moment a declaration can be stored, there are two candidates for what {@code issues} means: the
 * row somebody wrote and the {@code QuerySource} the product assembled in Java. If the projection screen
 * reads one of them and the listing runs the other, the screen is a lie — and it is the most convincing
 * kind, because both halves are real.</p>
 *
 * <p>So a product's query path resolves through here rather than reaching for its own builder, and the
 * fallback lives in one place: <strong>a row when there is one, the product's own otherwise</strong>.</p>
 *
 * <h2>⚠️ A DERIVED subject never consults the store, even if a row exists</h2>
 *
 * <p>An entry source is built from the fields somebody put on a form. There is nothing to author, so
 * there is nothing to prefer — and honouring a stale row for one would mean a query naming a field the
 * form no longer has, refused with a message about the field rather than about the row that named it.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class QuerySources {

    private static final JmqTranslator JMQ = new JmqTranslator();

    private final AuthoredSources  stored;
    private final QueryLanguage    language;
    private final PublishedTables  published;

    public QuerySources(AuthoredSources stored, QueryLanguage language, PublishedTables published) {
        this.stored    = stored;
        this.language  = language;
        this.published = published;
    }

    /**
     * The source this subject runs against for this request.
     *
     * @param subject which listing
     * @param request what was asked
     * @return the source, or empty when the subject declines to describe itself
     */
    public Optional<QuerySource> resolve(QuerySubject subject, QueryRequest request) {
        if (subject.origin() == SourceOrigin.AUTHORED) {
            Optional<QuerySource> authored = stored
                    .find(subject.declarationOwner(request), subject.name())
                    .map(source -> load(source.body()));

            if (authored.isPresent()) {
                return authored;
            }
        }

        return subject.source(request);
    }

    /**
     * What the editor opens on — the row if there is one, otherwise the product's own, projected.
     *
     * <p>⚠️ **Falling back to the projection rather than to an empty box is the whole ergonomics of
     * this.** Somebody authoring a declaration for the first time starts from what the product already
     * runs, which is both a working document and the answer to *what am I allowed to write here*. An
     * empty editor would make the first edit a rewrite from nothing.</p>
     */
    public Optional<Draft> draft(QuerySubject subject, QueryRequest request) {
        Optional<AuthoredSource> written = subject.origin() == SourceOrigin.AUTHORED
                ? stored.find(subject.declarationOwner(request), subject.name())
                : Optional.empty();

        if (written.isPresent()) {
            AuthoredSource source = written.get();

            return Optional.of(new Draft(source.body(), true, source.author(), source.updatedAt()));
        }

        return subject.source(request)
                .map(SourceLoader::declare)
                .map(declared -> new Draft(JMQ.translate(declared, Bindings.none()), false, null, null));
    }

    /**
     * Parses and vets a body, without storing it.
     *
     * <p>⚠️ Both checks, in this order, and neither may be skipped by a caller in a hurry: the language
     * says whether it is a declaration at all, and only a parsed declaration can be asked which tables it
     * reaches. Vetting the text first would be the string-matching this design exists to avoid.</p>
     *
     * @param body the jMQ
     * @return the parsed declaration, already vetted
     */
    public SourceNode vet(String body) {
        SourceNode declared = parse(body);

        published.require(declared);

        return declared;
    }

    /** ⚠️ Named separately from {@link #vet} so nothing can accidentally store an unvetted body. */
    private SourceNode parse(String body) {
        QueryDocumentNode document = language.document(body);

        if (document.getSources().isEmpty()) {
            throw new IllegalArgumentException(
                    "this is not a declaration — it needs a `structure` and a `mapping` (or a `source`)");
        }

        if (document.getSources().size() > 1) {
            throw new IllegalArgumentException(
                    "one declaration per source: this text describes %d"
                            .formatted(document.getSources().size()));
        }

        return document.getSources().getFirst();
    }

    /** ⚠️ Loading goes through the same reader a file does, so a row and a file cannot mean two things. */
    private QuerySource load(String body) {
        return SourceLoader.load(parse(body));
    }

    /**
     * What an editor is handed.
     *
     * @param body      the jMQ to open on
     * @param authored  whether this came from a row — {@code false} means it is the product's own,
     *                  projected, and saving it would be the first time anybody wrote one
     * @param author    who last wrote it, or {@code null} when nobody has
     * @param updatedAt when that was, or {@code null}
     */
    public record Draft(String body, boolean authored, String author, Instant updatedAt) {
    }
}
