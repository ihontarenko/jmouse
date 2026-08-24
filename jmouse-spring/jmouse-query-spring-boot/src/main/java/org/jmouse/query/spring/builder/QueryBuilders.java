package org.jmouse.query.spring.builder;

import org.jmouse.el.node.Expression;
import org.jmouse.query.compose.ConditionRow;
import org.jmouse.query.compose.QueryComposer;
import org.jmouse.query.compose.QueryDecomposer;
import org.jmouse.query.compose.RowOperators;
import org.jmouse.query.el.QueryLanguage;
import org.jmouse.query.schema.QueryAttribute;
import org.jmouse.query.schema.QueryChecker;
import org.jmouse.query.schema.QuerySchema;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The builder's two questions, answered for any subject.
 *
 * <h2>⚠️ Every path goes through the AST, in both directions</h2>
 *
 * <p>Rows become nodes and the nodes write themselves; text is parsed and the nodes are read. Nothing
 * here concatenates or matches a fragment of the language. That is the entire reason this exists on a
 * server rather than in each interface — the two directions written by hand in TypeScript drifted far
 * enough to turn a supplied value into a string literal, silently.</p>
 *
 * <h2>⚠️ A refusal is DATA here, not an exception</h2>
 *
 * <p>A screen asks on every keystroke, and half-typed text is the normal state of a box somebody is
 * typing into. So an unreadable query comes back as {@code readable: false} with the checker's own
 * sentence. What still throws is a caller being wrong — an operator nobody offers, a subject nobody
 * registered — because that is a bug rather than a person mid-thought.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class QueryBuilders {

    private final QuerySubjects subjects;
    private final QueryLanguage language;

    public QueryBuilders(QuerySubjects subjects) {
        this(subjects, new QueryLanguage());
    }

    public QueryBuilders(QuerySubjects subjects, QueryLanguage language) {
        this.subjects = subjects;
        this.language = language;
    }

    /** What a query written against this subject may name, and the comparisons it may use. */
    public QueryViews.Vocabulary describe(QueryRequest request) {
        QuerySubject                subject    = subjects.named(request.subject());

        subject.authorize(request);

        QuerySchema                 schema     = subject.schema(request);
        List<QueryViews.Attribute>  attributes = new ArrayList<>();

        Map<String, QuerySubject.Presentation> shownAs = subject.presentations(schema, request);

        for (QueryAttribute attribute : schema.attributes()) {
            QuerySubject.Presentation shown = shownAs.getOrDefault(
                    attribute.name(), new QuerySubject.Presentation(attribute.name(), List.of()));

            attributes.add(new QueryViews.Attribute(
                    attribute.name(),
                    shown.label(),
                    attribute.type().name().toLowerCase(),
                    attribute.access().name().toLowerCase(),
                    subject.converters().converterFor(attribute),
                    shown.options()));
        }

        return new QueryViews.Vocabulary(subject.name(), attributes, operators());
    }

    /**
     * Translates in whichever direction the request implies, and answers everything at once.
     *
     * <p>⚠️ The composed text is checked <strong>before</strong> it is returned, so the refusal a person
     * sees while composing and the one the listing would answer with are the same judgement made by the
     * same code.</p>
     */
    public QueryViews.Translated translate(QueryRequest request, QueryViews.Translation translation) {
        QuerySubject    subject    = subjects.named(request.subject());

        subject.authorize(request);

        QuerySchema     schema     = subject.schema(request);
        QueryComposer   composer   = new QueryComposer(schema, subject.converters());
        QueryDecomposer decomposer = new QueryDecomposer();

        String             filter;
        List<ConditionRow> rows;

        try {
            if (translation.rows() != null) {
                filter = composer.filter(translation.rows());
                rows   = translation.rows();
            } else {
                filter = translation.filter() == null ? "" : translation.filter().trim();
                rows   = filter.isEmpty()
                        ? List.of()
                        : decomposer.rows(language.expression(filter)).orElse(null);
            }
        } catch (RuntimeException refused) {
            return new QueryViews.Translated(
                    translation.filter(), "", null, false, refused.getMessage());
        }

        String order;

        try {
            order = composer.order(translation.orderBy(), translation.descends());
        } catch (RuntimeException refused) {
            return new QueryViews.Translated(filter, "", rows, false, refused.getMessage());
        }

        Optional<String> refusal = refusal(filter, schema, subject.values(request));

        return new QueryViews.Translated(
                filter, order, rows, refusal.isEmpty(), refusal.orElse(null));
    }

    /**
     * ⚠️ Checked against the <strong>schema</strong> rather than compiled to SQL, deliberately. This
     * surface serves any adapter — a list of maps, a file, a database — and compiling would tie a
     * builder to the one backend that happens to be underneath today.
     */
    private Optional<String> refusal(String filter, QuerySchema schema, Map<String, Object> values) {
        if (filter == null || filter.isBlank()) {
            return Optional.empty();
        }

        try {
            Expression parsed = language.expression(filter);

            new QueryChecker(schema, values.keySet()).checkCondition(parsed);

            return Optional.empty();
        } catch (RuntimeException | Error refused) {
            return Optional.of(refused.getMessage());
        }
    }

    /**
     * ⚠️ Sent rather than hard-coded in a screen. A builder offering a comparison the composer does not
     * have produces a refusal about an operator the person was handed by that same screen.
     */
    private List<QueryViews.Operator> operators() {
        List<QueryViews.Operator> offered = new ArrayList<>();

        for (RowOperators operator : RowOperators.values()) {
            offered.add(new QueryViews.Operator(
                    operator.spelling(), operator.needsValue(), operator.ordered(), operator.negative()));
        }

        return offered;
    }
}
