package org.jmouse.query.compose.smoke;

import org.jmouse.el.node.Expression;
import org.jmouse.query.compose.ConditionRow;
import org.jmouse.query.compose.ConverterPolicy;
import org.jmouse.query.compose.QueryComposer;
import org.jmouse.query.compose.QueryDecomposer;
import org.jmouse.query.el.QueryLanguage;
import org.jmouse.query.schema.QueryAttribute;
import org.jmouse.query.schema.QuerySchema;
import org.jmouse.query.schema.QueryType;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Rows in, jMQ out, and back again — printed, so the round trip can be read rather than asserted.
 *
 * <p>⚠️ A <strong>demonstration</strong>, not a test. The thing worth seeing here is the pair of lines
 * for each case: what a builder holds, and what the language makes of it. The defect this whole package
 * exists to prevent — a value written back out as something it was not — is visible in that pair and
 * invisible in a green check.</p>
 *
 * <p>Run it: {@code mvn -q -pl jmouse-query exec:java -Dexec.mainClass=org.jmouse.query.compose.smoke.ComposeSmoke}</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class ComposeSmoke {

    private static final PrintStream OUT = new PrintStream(System.out, true, StandardCharsets.UTF_8);

    private static int passed;
    private static int failed;

    /**
     * ⚠️ A bag value is {@code UNKNOWN}, which is what makes it need a converter — the case the whole
     * converter mechanism exists for, and the one a hand-written writer got wrong.
     */
    private static final QuerySchema SCHEMA = schema(
            new QueryAttribute("entry[quantity]", "f-quantity", QueryType.UNKNOWN, QueryAttribute.Access.BAG),
            new QueryAttribute("entry[name]", "f-name", QueryType.TEXT, QueryAttribute.Access.BAG),
            new QueryAttribute("asset[state]", "state", QueryType.TEXT, QueryAttribute.Access.COLUMN),
            new QueryAttribute("submitter", "submitter_id", QueryType.TEXT, QueryAttribute.Access.COLUMN),
            new QueryAttribute("created", "created_at", QueryType.TEMPORAL, QueryAttribute.Access.COLUMN));

    private static final ConverterPolicy CONVERTERS =
            attribute -> attribute.needsConverterForOrdering() ? "int" : null;

    public static void main(String[] arguments) {
        QueryComposer   composer   = new QueryComposer(SCHEMA, CONVERTERS);
        QueryDecomposer decomposer = new QueryDecomposer();
        QueryLanguage   language   = new QueryLanguage();

        OUT.println("── Rows become jMQ ─────────────────────────────────────────────");

        writes(composer, "a plain equality",
                List.of(new ConditionRow("asset[state]", "equals", "AVAILABLE")),
                "( asset[state] == 'AVAILABLE' )");

        writes(composer, "an untyped value ordered — the converter is placed for you",
                List.of(new ConditionRow("entry[quantity]", "less", 10)),
                "( entry[quantity] | int < 10 )");

        writes(composer, "two rows join with and",
                List.of(new ConditionRow("asset[state]", "equals", "AVAILABLE"),
                        new ConditionRow("entry[quantity]", "greater", 0)),
                "( ( asset[state] == 'AVAILABLE' ) and ( entry[quantity] | int > 0 ) )");

        writes(composer, "a test",
                List.of(new ConditionRow("entry[name]", "contains", "stm")),
                "entry[name] is contains('stm')");

        writes(composer, "and the absence question beside a negative one",
                List.of(new ConditionRow("entry[name]", "notContains", "stm", true)),
                "( entry[name] is null or entry[name] is not contains('stm') )");

        writes(composer, "nothing to compare with",
                List.of(new ConditionRow("entry[name]", "empty", null)),
                "entry[name] is null");

        writes(composer, "an unfinished row is skipped, not refused",
                List.of(new ConditionRow("entry[name]", "contains", ""),
                        new ConditionRow("asset[state]", "equals", "ISSUED")),
                "( asset[state] == 'ISSUED' )");

        writes(composer, "⚠️ a value that looks numeric but is not, is not made one",
                List.of(new ConditionRow("entry[name]", "equals", "0603")),
                "( entry[name] == '0603' )");

        OUT.println();
        OUT.println("── jMQ becomes rows again ──────────────────────────────────────");

        reads(language, decomposer, "asset[state] == 'AVAILABLE'", 1);
        reads(language, decomposer, "entry[quantity] | int < 10", 1);
        reads(language, decomposer, "entry[name] is contains('stm') and asset[state] != 'ISSUED'", 2);
        reads(language, decomposer, "(entry[name] is null or entry[name] is not contains('stm'))", 1);

        OUT.println();
        OUT.println("── ⚠️ And what it REFUSES to draw, which is the point ───────────");

        refuses(language, decomposer, "submitter == currentMember",
                "a supplied value is not a literal");
        refuses(language, decomposer, "created > now() - days(7)",
                "an expression is not a value box");
        refuses(language, decomposer, "asset[state] == 'ISSUED' or asset[state] == 'AVAILABLE'",
                "a row of controls cannot show precedence");
        refuses(language, decomposer, "entry[name] is contains('a', 'b')",
                "two arguments, one value box");

        OUT.println();
        OUT.println("── Round trip ──────────────────────────────────────────────────");

        roundTrip(composer, decomposer, language, "asset[state] == 'AVAILABLE'");
        roundTrip(composer, decomposer, language, "entry[name] is contains('stm')");
        roundTrip(composer, decomposer, language, "entry[quantity] | int < 10");

        OUT.println();
        OUT.printf("%d passed, %d failed%n", passed, failed);

        if (failed > 0) {
            System.exit(1);
        }
    }

    /**
     * ⚠️ The expected text carries the un-parse's own brackets. {@code BinaryOperation} parenthesises
     * unconditionally rather than by precedence, so a single comparison comes back as {@code ( a == b )}.
     * It re-parses identically and is correct; it is only noisier than a person would write. Making it
     * precedence-aware changes every dialect's un-parse, so it has a ticket of its own rather than a
     * quiet edit here.
     */
    private static void writes(QueryComposer composer, String what, List<ConditionRow> rows, String wanted) {
        String written = composer.filter(rows);

        report(what, wanted, written);
    }

    private static void reads(
            QueryLanguage language, QueryDecomposer decomposer, String filter, int wanted) {

        Optional<List<ConditionRow>> rows = decomposer.rows(language.expression(filter));

        if (rows.isEmpty()) {
            report(filter, wanted + " row(s)", "refused");
            return;
        }

        report(filter, wanted + " row(s)", rows.get().size() + " row(s)");
        rows.get().forEach(row -> OUT.printf("        %s %s %s%s%n",
                row.attribute(), row.operator(),
                row.value() == null ? "" : row.value(),
                row.includeMissing() ? "  (+ missing)" : ""));
    }

    private static void refuses(
            QueryLanguage language, QueryDecomposer decomposer, String filter, String why) {

        Optional<List<ConditionRow>> rows = decomposer.rows(language.expression(filter));

        report(filter + "  — " + why, "refused", rows.isEmpty() ? "refused" : "drawn as rows");
    }

    /**
     * ⚠️ The one that matters: text → rows → text has to land on the same query. Anything else means a
     * person's filter changes the moment they touch a control.
     */
    private static void roundTrip(
            QueryComposer composer, QueryDecomposer decomposer, QueryLanguage language, String filter) {

        Expression                   parsed = language.expression(filter);
        Optional<List<ConditionRow>> rows   = decomposer.rows(parsed);

        if (rows.isEmpty()) {
            report(filter, "recognised", "refused");
            return;
        }

        String again = composer.filter(rows.get());

        report(filter, normalised(filter), normalised(again));
    }

    /** Brackets and spacing are the un-parse's, so compare what the query <em>says</em>. */
    private static String normalised(String filter) {
        return filter.replace("(", "").replace(")", "").replaceAll("\\s+", " ").trim();
    }

    private static void report(String what, String wanted, String got) {
        boolean same = wanted.equals(got);

        if (same) {
            passed++;
        } else {
            failed++;
        }

        OUT.printf("  %s %s%n", same ? "✓" : "✗", what);

        if (!same) {
            OUT.printf("        wanted: %s%n        got:    %s%n", wanted, got);
        } else if (!wanted.endsWith("row(s)") && !wanted.equals("refused")) {
            OUT.printf("        %s%n", got);
        }
    }

    private static QuerySchema schema(QueryAttribute... attributes) {
        Map<String, QueryAttribute> byName = new LinkedHashMap<>();

        for (QueryAttribute attribute : attributes) {
            byName.put(attribute.name(), attribute);
        }

        return new QuerySchema() {

            @Override
            public Optional<QueryAttribute> attribute(String name) {
                return Optional.ofNullable(byName.get(name));
            }

            @Override
            public Collection<QueryAttribute> attributes() {
                return byName.values();
            }
        };
    }

    private ComposeSmoke() {
    }
}
