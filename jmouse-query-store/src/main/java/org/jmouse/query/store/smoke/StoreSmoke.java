package org.jmouse.query.store.smoke;

import org.jmouse.query.el.QueryLanguage;
import org.jmouse.query.schema.QueryAttribute;
import org.jmouse.query.schema.QuerySchema;
import org.jmouse.query.schema.QueryType;
import org.jmouse.query.store.QueryLibrary;
import org.jmouse.query.store.QueryOwner;
import org.jmouse.query.store.SavedQuery;
import org.jmouse.query.store.SavedQueryCriteria;
import org.jmouse.query.store.SavedQueryDraft;
import org.jmouse.query.store.SchemaCatalog;
import org.jmouse.query.store.memory.MemorySavedQueries;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.jmouse.query.schema.QueryAttribute.Access.BAG;
import static org.jmouse.query.schema.QueryAttribute.Access.COLUMN;

/**
 * Queries somebody kept — saving, refusing, and who sees what.
 *
 * <pre>
 *   mvn -pl jmouse-query-store exec:java -Dexec.mainClass=org.jmouse.query.store.smoke.StoreSmoke
 * </pre>
 *
 * <h2>⚠️ No database, and that is the point of the port</h2>
 *
 * <p>Everything below runs over {@link MemorySavedQueries}. What is being demonstrated is
 * {@link QueryLibrary} — the checking that stands in front of <em>every</em> backend — and a
 * demonstration that needed a schema to prove it would have proved something about JPA instead.</p>
 *
 * <h2>⚠️ Checked on SAVE, never on read</h2>
 *
 * <p>A row that cannot compile is a row that fails for every viewer, every time, and by then the person
 * who knew what they meant is long gone. So the refusal happens while they are still typing, in the
 * compiler's own words.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class StoreSmoke {

    private static final QueryOwner BOARD  = QueryOwner.of("BOARD", "board-1");
    private static final QueryOwner PERSON = QueryOwner.of("MEMBER", "member-7");

    private static int passed;
    private static int failed;

    static {
        System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out), true, StandardCharsets.UTF_8));
    }

    private StoreSmoke() {
    }

    public static void main(String[] arguments) {
        QuerySchema issues = schema(
                new QueryAttribute("issue.key", "issue_key", QueryType.TEXT, COLUMN),
                new QueryAttribute("issue.points", "story_points", QueryType.NUMBER, COLUMN),
                new QueryAttribute("issue.status", "status_id", QueryType.TEXT, COLUMN),
                new QueryAttribute("issue.assignee", "assignee_member_id", QueryType.TEXT, COLUMN),
                new QueryAttribute("entry[quantity]", "f-quantity", QueryType.UNKNOWN, BAG));

        QueryLibrary library = new QueryLibrary(
                new MemorySavedQueries(), new QueryLanguage(),
                SchemaCatalog.of(Map.of("issues", issues)));

        banner("1 · A FILTER SOMEBODY KEPT — one condition, not a whole view");
        SavedQuery mine = library.save(new SavedQueryDraft(
                "issues", BOARD, "Моє в роботі", "те, що я тягну зараз",
                "issue.assignee == 'member-7' and issue.status != 'status-done'",
                "member-7", false, 0));

        say(mine.getName() + "  ·  " + mine.getIdentifier());
        say("   " + mine.getBody());

        banner("2 · AND A WHOLE VIEW — the same table, the same column");
        SavedQuery report = library.save(new SavedQueryDraft(
                "issues", BOARD, "Вага за статусом", null,
                """
                view "Вага за статусом" on issues {
                  columns issue.status as status, count() as many, sum(issue.points) as points
                  group   issue.status
                  having  sum(issue.points) > 4
                  order   sum(issue.points) desc
                }
                """,
                "member-7", true, 1));

        say(report.getName() + "  ·  shared=" + report.isShared());
        say("   ⚠️ which of the two shapes it is in is NOT recorded — the text already says it, and a");
        say("      second statement of that would disagree the first time a filter grew into a view.");

        banner("⚠️ 3 · REFUSED AT SAVE TIME, IN THE COMPILER'S OWN WORDS");
        refuse("an attribute nothing declares", () -> library.check(draft(
                "issue.asignee == 'member-7'")));
        refuse("an ordered comparison over a bag with no converter", () -> library.check(draft(
                "entry[quantity] > 100")));
        refuse("an aggregate in a row filter", () -> library.check(draft(
                "count() > 3")));
        refuse("a view that reads `on` something else than the row's source", () -> library.check(draft(
                "view \"v\" on inventory { where entry[quantity] | int > 1 columns entry[quantity] as q }")));
        refuse("a source nothing in this installation describes", () -> library.save(new SavedQueryDraft(
                "invoices", BOARD, "Рахунки", null, "issue.key == 'X'", "member-7", false, 0)));
        refuse("a body longer than the cap", () -> library.check(draft(
                "issue.key == '" + "x".repeat(SavedQueryDraft.MAXIMUM_BODY_LENGTH) + "'")));

        banner("4 · WHO SEES WHAT — shared, plus your own");
        library.save(new SavedQueryDraft("issues", BOARD, "Тільки моє", null,
                "issue.assignee == 'member-9'", "member-9", false, 2));

        List<SavedQuery> forSeven = library.list(SavedQueryCriteria.ownedBy(BOARD).on("issues").seenBy("member-7"));
        List<SavedQuery> forNine = library.list(SavedQueryCriteria.ownedBy(BOARD).on("issues").seenBy("member-9"));

        forSeven.forEach(query -> say("member-7 sees: " + query.getName()));
        forNine.forEach(query -> say("member-9 sees: " + query.getName()));

        note(forSeven.size() == 2 && forNine.size() == 2,
                "each sees the shared one and their own, and neither sees the other's private one");

        banner("⚠️ 5 · THE ROW CARRIES NO SCOPE — the owner is the product's own word");
        say("owner: " + BOARD.type() + " / " + BOARD.identifier()
            + "   ·   installation: " + QueryOwner.installation().type());
        say("   ⚠️ not an enum of PERSONAL | PROJECT | GLOBAL: every product hangs saved queries off");
        say("      something different, and an enum here would mean releasing the library each time one");
        say("      of them found a new thing to hang them off.");

        banner("6 · UPDATING KEEPS THE IDENTIFIER — a board pointing at it follows the edit");
        SavedQuery edited = library.update(mine.getIdentifier(), new SavedQueryDraft(
                "issues", BOARD, "Моє в роботі", "звужено до великих",
                "issue.assignee == 'member-7' and issue.points > 4",
                "member-7", false, 0));

        note(edited.getIdentifier().equals(mine.getIdentifier()), "same identifier: " + edited.getIdentifier());
        note(!edited.getBody().equals(mine.getBody()), "new body: " + edited.getBody());

        banner("⚠️ 7 · AND A PREVIEW ASKS EXACTLY WHAT A SAVE ASKS");
        library.check(new SavedQueryDraft("issues", PERSON, "проба", null,
                "issue.points > 4", "member-7", false, 0));
        note(true, "check() is public, so a screen shows the same sentence while somebody types");

        System.out.printf("%n=== %d passed, %d failed%n", passed, failed);
    }

    private static SavedQueryDraft draft(String body) {
        return new SavedQueryDraft("issues", BOARD, "проба", null, body, "member-7", false, 0);
    }

    private static void banner(String title) {
        System.out.println();
        System.out.println("══ " + title);
    }

    private static void say(String line) {
        System.out.println("  " + line);
    }

    private static void note(boolean ok, String line) {
        if (ok) {
            passed++;
        } else {
            failed++;
        }

        System.out.println("  " + (ok ? "ok  " : "<<< ") + line);
    }

    private static void refuse(String what, Runnable action) {
        try {
            action.run();
            failed++;
            System.out.println("  " + what + "  <<< НЕ ВІДМОВЛЕНО");
        } catch (Throwable refusal) {
            passed++;
            System.out.println("  ✗ " + what);
            System.out.println("    " + refusal.getMessage());
        }
    }

    private static QuerySchema schema(QueryAttribute... attributes) {
        Map<String, QueryAttribute> known = new LinkedHashMap<>();

        for (QueryAttribute attribute : attributes) {
            known.put(attribute.name(), attribute);
        }

        return new QuerySchema() {

            @Override
            public Optional<QueryAttribute> attribute(String name) {
                return Optional.ofNullable(known.get(name));
            }

            @Override
            public Collection<QueryAttribute> attributes() {
                return known.values();
            }
        };
    }
}
