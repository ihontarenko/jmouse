package org.jmouse.query.sql.smoke;

import org.jmouse.query.translate.Capabilities;
import org.jmouse.query.translate.Capability;
import org.jmouse.query.translate.Translator;
import org.jmouse.query.translate.row.RowTranslator;
import org.jmouse.query.el.QueryLanguage;
import org.jmouse.query.el.node.ViewNode;
import org.jmouse.query.model.QueryModel;
import org.jmouse.query.model.QueryProjection;
import org.jmouse.query.sql.Fragment;
import org.jmouse.query.sql.QueryEngine;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.jmouse.query.sql.smoke.Smokes.TESSERA;
import static org.jmouse.query.sql.smoke.Smokes.banner;
import static org.jmouse.query.sql.smoke.Smokes.jmq;
import static org.jmouse.query.sql.smoke.Smokes.note;
import static org.jmouse.query.sql.smoke.Smokes.refuse;
import static org.jmouse.query.sql.smoke.Smokes.summary;

/**
 * Two backends, one query — and the AST's third output.
 *
 * <pre>
 *   mvn -pl jmouse-query-sql exec:java -Dexec.mainClass=org.jmouse.query.sql.smoke.AdapterSmoke
 * </pre>
 *
 * <p>⚠️ The point of the first section is not that both work. It is that they <strong>agree</strong> —
 * until there were two backends, nobody could tell whether a query meant one thing or two.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class AdapterSmoke {

    private static final String VIEW = """
            view "Велике й закрите" on issues {
              where   issue.points > 8 and issue.status == 'status-done'
              columns issue.key as k, issue.points as pts
              order   issue.points desc, issue.key asc
            }
            """;

    public static void main(String[] arguments) throws Exception {
        QueryEngine tssr = Catalog.tessera();
        QueryLanguage language = new QueryLanguage();
        ViewNode view = language.document(VIEW).getViews().getFirst();

        banner("⚠️ 1 · THE SAME QUERY, TWO BACKENDS — do they AGREE?");
        jmq(VIEW);

        // ── the SQL backend, against the live database
        Fragment statement = tssr.compileDocument(VIEW);
        List<Map<String, Object>> fromDatabase = execute(statement);

        System.out.println("  SQL ── " + fromDatabase.size() + " rows");
        fromDatabase.stream().limit(3).forEach(row -> System.out.println("     " + row));

        // ── the in-memory backend, over the SAME rows fetched raw
        List<Map<String, Object>> raw = execute(Fragment.of(
                "SELECT issue_key, story_points, status_id FROM issues"));
        List<Map<String, Object>> asAttributes = new ArrayList<>();

        for (Map<String, Object> row : raw) {
            Map<String, Object> attributes = new LinkedHashMap<>();

            attributes.put("issue.key", row.get("issue_key"));
            attributes.put("issue.points", row.get("story_points"));
            attributes.put("issue.status", row.get("status_id"));
            asAttributes.add(attributes);
        }

        RowTranslator memory = new RowTranslator(Catalog.ISSUES);
        List<Map<String, Object>> fromMemory = memory.translate(view).run(asAttributes);

        System.out.println("  MEMORY ── " + fromMemory.size() + " rows");
        fromMemory.stream().limit(3).forEach(row -> System.out.println("     " + row));

        note(fromDatabase.size() == fromMemory.size(),
                "both backends returned %d rows".formatted(fromMemory.size()));
        note(sameOrder(fromDatabase, fromMemory), "…in the same order, key for key");

        banner("⚠️ 2 · AN ADAPTER REFUSES WHAT IT CANNOT DO — it does not quietly drop it");
        refuse(() -> memory.translate(language.document("""
                view "v" on issues {
                  columns issue.status as st, count() as many
                  group   issue.status
                }
                """).getViews().getFirst()));

        note(!memory.capabilities().has(Capability.AGGREGATE),
                "the memory adapter declares no AGGREGATE, and says so by name");

        banner("3 · THE THIRD OUTPUT — the same AST as plain data");
        QueryModel model = QueryProjection.project(view);

        System.out.println("     title    : " + model.title());
        System.out.println("     target   : " + model.target());
        System.out.println("     where    : " + model.where());
        System.out.println("     columns  : " + model.columns());
        System.out.println("     order    : " + model.order());
        System.out.println("     grouped  : " + model.grouped());

        note(model.where().contains("issue.points"), "the condition comes back as the language's own text");
        note(model.order().size() == 2 && model.order().get(0).descending(),
                "sort keys carry direction as a boolean, not as the written word");

        banner("⚠️ 4 · THE MODEL IS DERIVED — its text re-parses to the same query");
        String rebuilt = "view \"%s\" on %s { where %s }".formatted(
                model.title(), model.target(), model.where());

        note(language.document(rebuilt).getViews().size() == 1,
                "the model's `where` is text the parser accepts again");

        banner("5 · A GROUPED VIEW REPORTS ITSELF AS GROUPED");
        QueryModel report = QueryProjection.project(language.document("""
                view "Скільки" on issues {
                  columns issue.status as st, count() as many
                  group   issue.status
                  having  count() > 10
                }
                """).getViews().getFirst());

        System.out.println("     group   : " + report.group());
        System.out.println("     having  : " + report.having());
        note(report.grouped(), "grouped = true — a row of this result is a TUPLE, and paging counts groups");

        banner("⚠️ 6 · BOTH BACKENDS REACHED AS ONE TYPE — Translator<?>, decided at run time");

        // ⚠️ The SQL backend now sits ON the seam rather than beside it. Until it did, a product holding
        // a configured engine held something SQL-only: there was no Translator<?> it could be handed
        // instead, and "one language, several backends" was true of the language rather than of the
        // thing anybody uses.
        List<Translator<?>> backends = List.of(tssr.translator("issues"), memory);

        for (Translator<?> backend : backends) {
            Capabilities declared = backend.capabilities();

            System.out.printf("     %-8s aggregate=%-5s join=%-5s clock=%s%n",
                    declared.translator(),
                    declared.has(Capability.AGGREGATE),
                    declared.has(Capability.JOIN),
                    declared.has(Capability.CLOCK));

            note(backend.translate(view) != null,
                    "%s compiled the same view, through the same interface".formatted(declared.translator()));
        }

        ViewNode grouped = language.document("""
                view "Скільки" on issues {
                  columns issue.status as st, count() as many
                  group   issue.status
                }
                """).getViews().getFirst();

        note(tssr.translator("issues").translate(grouped) != null, "the SQL translator groups");
        refuse(() -> memory.translate(grouped));
        note(true, "…and the memory one refuses by name rather than returning ungrouped rows");

        summary();
    }

    private static boolean sameOrder(List<Map<String, Object>> left, List<Map<String, Object>> right) {
        if (left.size() != right.size()) {
            return false;
        }

        for (int index = 0; index < left.size(); index++) {
            Object one = left.get(index).values().iterator().next();
            Object two = right.get(index).values().iterator().next();

            if (!String.valueOf(one).equals(String.valueOf(two))) {
                return false;
            }
        }

        return true;
    }

    private static List<Map<String, Object>> execute(Fragment statement) throws Exception {
        List<Map<String, Object>> collected = new ArrayList<>();

        try (Connection connection = DriverManager.getConnection(TESSERA, "tessera", "tessera");
             PreparedStatement prepared = connection.prepareStatement(statement.sql())) {

            List<Object> values = statement.parameters();

            for (int index = 0; index < values.size(); index++) {
                prepared.setObject(index + 1, values.get(index));
            }

            try (ResultSet rows = prepared.executeQuery()) {
                while (rows.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();

                    for (int column = 1; column <= rows.getMetaData().getColumnCount(); column++) {
                        row.put(rows.getMetaData().getColumnLabel(column), rows.getObject(column));
                    }

                    collected.add(row);
                }
            }
        }

        return collected;
    }
}
