package org.jmouse.query.spring.playground;

import org.jmouse.jdbc.dialect.Dialects;
import org.jmouse.query.sql.QuerySource;
import org.jmouse.query.spring.builder.QueryCallers;
import org.jmouse.query.spring.builder.QueryRequest;
import org.jmouse.query.spring.builder.QueryRoutes;
import org.jmouse.query.spring.builder.QueryRunner;
import org.jmouse.query.spring.builder.QuerySubject;
import org.jmouse.query.spring.builder.QuerySubjects;
import org.jmouse.query.spring.source.QuerySources;
import org.jmouse.el.translate.Bindings;
import org.jmouse.el.translate.Capability;
import org.jmouse.query.translate.JmqTranslator;
import org.jmouse.query.translate.JsonTranslator;
import org.jmouse.el.translate.Translator;
import org.jmouse.query.translate.XmlTranslator;
import org.jmouse.query.translate.row.RowTranslator;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * What a query compiles to — the statement, never the rows.
 *
 * <h2>⚠️ It COMPILES and does not execute, and that is a boundary rather than a shortcut</h2>
 *
 * <p>{@code QuerySubject} exists for composing and reading a query — the half that is identical in every
 * product. Running one needs a scope built from the session, paging, and a way to load whatever matched,
 * and not one of those is the same in two products. A shared controller that ran queries would have to
 * invent all three, which is a library guessing at a product's authorization.</p>
 *
 * <p>Compiling needs none of it. So the honest thing a library can offer is <em>this is the question you
 * asked, in SQL</em> — which is also the half that answers the question somebody actually has when they
 * are checking a mapping: <em>did my declaration produce the join I meant?</em></p>
 *
 * <h2>⚠️ The statement is incomplete ON PURPOSE, and the screen has to say so</h2>
 *
 * <p>A listing adds its own confinement before running — the projects this caller may browse, the
 * workspace they are in — as a fragment supplied at the call site. It is absent here because it is not
 * this module's to write. Presenting the result as the statement the listing runs would teach a reader
 * that their listing is unconfined, which is both false and dangerous to believe.</p>
 *
 * <h2>⚠️ A refusal is DATA</h2>
 *
 * <p>Half-typed jMQ is the normal state of a box somebody is typing into, so an unreadable query comes
 * back as {@code readable: false} with the compiler's own sentence. Raising it would flash an error at
 * somebody mid-word.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
@RestController
@RequestMapping(QueryRoutes.PREFIX)
public class PlaygroundController {

    /**
     * ⚠️ Stateless, so one of each is right — and constructed rather than injected because a translator
     * into a language has nothing to configure. Making them beans would invite a product to replace one,
     * and a product with its own idea of how jMQ is spelled is what this seam exists to prevent.
     */
    private static final JmqTranslator  JMQ  = new JmqTranslator();
    private static final JsonTranslator JSON = new JsonTranslator();
    private static final XmlTranslator  XML  = new XmlTranslator();

    private final QuerySubjects subjects;
    private final QueryCallers  callers;
    private final QuerySources  sources;
    private final QueryRunner   runner;

    public PlaygroundController(QuerySubjects subjects, QueryCallers callers,
                                QuerySources sources, QueryRunner runner) {
        this.subjects = subjects;
        this.callers  = callers;
        this.sources  = sources;
        this.runner   = runner;
    }

    /**
     * Compiles a query against this subject and hands back the statement.
     *
     * <p>⚠️ Against the <strong>resolved</strong> declaration, so what is shown is compiled from the
     * source the listing would really use — a preview compiled from the shipped declaration while the
     * listing ran a stored one would be a preview of a different query.</p>
     */
    @PostMapping("/{subject}/playground")
    @Transactional(readOnly = true)
    public Compiled compile(@PathVariable String subject,
                            @RequestParam Map<String, String> parameters,
                            @RequestBody Asked asked) {
        QuerySubject named   = subjects.named(subject);
        QueryRequest request = new QueryRequest(subject, callers.current(), parameters);

        // ⚠️ The READ gate. A compiled statement discloses the table and column names the mapping uses,
        // which is the same disclosure the declaration itself is — so it answers to the same question.
        named.authorizeSourceRead(request);

        return sources.resolve(named, request)
                .map(source -> render(source, asked, named.values(request)))
                .orElseGet(() -> Compiled.refused(
                        "'%s' does not describe itself, so nothing can be compiled against it"
                                .formatted(named.name())));
    }

    /**
     * One tree, whichever destination was asked for.
     *
     * <h2>⚠️ This is {@code Translator} being what it says it is</h2>
     *
     * <p>The seam's whole claim is that compiling for a vendor and writing back out as jMQ are the same
     * operation with a different destination. A screen that can switch between them is the cheapest
     * possible proof of that — and the cheapest possible way to notice the day it stops being true.</p>
     */
    private Compiled render(QuerySource source, Asked asked, java.util.Map<String, Object> values) {
        try {
            return switch (asked.destination()) {
                case JMQ -> written(source, asked, JMQ, "jmq");
                case JSON -> written(source, asked, JSON, "json");
                case XML -> written(source, asked, XML, "xml");
                case ROWS -> rows(source, asked);
                case SQL -> sql(source, asked, values);
            };
        } catch (RuntimeException refused) {
            return Compiled.refused(refused.getMessage());
        }
    }

    /**
     * Back into jMQ — the query as it was written.
     *
     * <p>⚠️ <strong>Nothing is bound and nothing is substituted</strong>, which is not an omission: a
     * translator back into the language must leave names as names. A query rendered with somebody's
     * identifier baked into it reads correctly and is wrong for everybody else — and it would be the
     * text a person copies into a saved view.</p>
     */
    private Compiled written(QuerySource source, Asked asked, Translator<String> translator, String language) {
        String rendered = translator.translate(
                runner.compose(source, asked.filter(), asked.order()), Bindings.none());

        return new Compiled(true, null, rendered, language, List.of(), null, true, List.of());
    }

    /**
     * Over rows in memory — and there is no text to show, so it does not invent any.
     *
     * <p>⚠️ This destination produces a <strong>pipeline</strong>, not a statement. The honest thing to
     * report is that it compiled and what it is able to honour: a backend over CSV or over objects cannot
     * group or aggregate, and a screen that printed something SQL-shaped here would suggest otherwise.</p>
     */
    private Compiled rows(QuerySource source, Asked asked) {
        RowTranslator translator = new RowTranslator(source.schema());

        translator.translate(runner.compose(source, asked.filter(), asked.order()), Bindings.none());

        return new Compiled(
                true, null, null, "rows", List.of(), null, true,
                // ⚠️ `Capability` is a RECORD wrapping a name, not an enum — so this reads the name
                // rather than calling `Enum::name`. It is a record precisely so a backend can declare a
                // capability the language has never heard of.
                translator.capabilities().all().stream()
                        .map(Capability::name).sorted().toList());
    }

    /** For a vendor — the connection's dialect, or one the caller named as a preview. */
    private Compiled sql(QuerySource source, Asked asked, java.util.Map<String, Object> values) {
        boolean named = asked.dialect() != null && !asked.dialect().isBlank();

        QueryRunner.Explained explained = named
                ? runner.explain(source, asked.filter(), asked.order(), values,
                                 Dialects.of(asked.dialect()))
                : runner.explain(source, asked.filter(), asked.order(), values);

        // ⚠️ `live` is how the screen knows to say "this is not what runs here". Compiling for an engine
        // this installation is not pointed at is a real question with a misleading answer if unlabelled:
        // the interval syntax differs, and that is a query that runs and answers about a different length
        // of time rather than one that fails.
        return new Compiled(true, null, explained.sql(), "sql", explained.parameters(),
                            explained.dialect(), explained.dialect().equals(runner.engine()), List.of());
    }

    /** Where a tree can be sent. */
    public enum Destination {
        /** SQL for a vendor. */
        SQL,
        /** Back into jMQ — the language rendering itself. */
        JMQ,
        /** A pipeline over rows in memory. */
        ROWS,
        /**
         * The tree as JSON.
         *
         * <p>⚠️ For LOOKING at, never for reading back — nothing parses it and nothing will. See
         * {@link org.jmouse.query.translate.Outline}.</p>
         */
        JSON,
        /** The tree as XML, with the same caveat as {@link #JSON}. */
        XML
    }

    /**
     * The jMQ to compile, and where to send it.
     *
     * @param filter      the condition, or blank — an empty query compiles to *everything*
     * @param order       the sort, or blank
     * @param translator  {@code sql} (the default), {@code jmq} or {@code rows}
     * @param dialect     for {@code sql} only: {@code mysql}, {@code postgresql}, or blank for the
     *                    connection's own
     */
    public record Asked(String filter, String order, String translator, String dialect) {

        /** ⚠️ Unknown spellings fall back to SQL rather than refusing — a destination is a view control,
         *  and a typo in one should not read as the query being broken. */
        Destination destination() {
            return switch (translator == null ? "" : translator.toLowerCase()) {
                case "jmq" -> Destination.JMQ;
                case "json" -> Destination.JSON;
                case "xml" -> Destination.XML;
                case "rows", "row" -> Destination.ROWS;
                default -> Destination.SQL;
            };
        }
    }

    /**
     * What the tree became, or why it did not.
     *
     * @param readable     whether it compiled — ⚠️ {@code false} is data, not a failure
     * @param message      the compiler's own sentence when it did not
     * @param output       the rendered form; {@code null} where the destination produces no text
     * @param language     what {@code output} is written in — {@code sql}, {@code jmq}, {@code rows}
     * @param parameters   what would be bound, in order; empty for a destination that binds nothing
     * @param dialect      which engine SQL was written for
     * @param live         ⚠️ whether this is what would actually run here — {@code false} for a preview
     *                     compiled against a dialect this installation is not pointed at
     * @param capabilities what the destination can honour, where that is the interesting answer
     */
    public record Compiled(boolean readable, String message, String output, String language,
                           List<String> parameters, String dialect, boolean live,
                           List<String> capabilities) {

        static Compiled refused(String message) {
            return new Compiled(false, message, null, null, List.of(), null, true, List.of());
        }
    }
}
