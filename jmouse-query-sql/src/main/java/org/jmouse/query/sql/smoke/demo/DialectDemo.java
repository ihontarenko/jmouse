package org.jmouse.query.sql.smoke.demo;

import org.jmouse.jdbc.dialect.PostgreSqlDialect;
import org.jmouse.query.sql.Fragment;
import org.jmouse.query.sql.QueryEngine;

import java.util.List;
import java.util.Map;

import static org.jmouse.query.sql.smoke.demo.Demo.engine;
import static org.jmouse.query.sql.smoke.demo.Demo.note;
import static org.jmouse.query.sql.smoke.demo.Demo.say;
import static org.jmouse.query.sql.smoke.demo.Demo.scenario;
import static org.jmouse.query.sql.smoke.demo.Demo.section;

/**
 * The same query, twice — MySQL and PostgreSQL side by side.
 *
 * <pre>
 *   mvn -pl jmouse-query-sql exec:java -Dexec.mainClass=org.jmouse.query.sql.smoke.demo.DialectDemo
 * </pre>
 *
 * <h2>⚠️ Where a database's own spelling actually enters</h2>
 *
 * <p>In eleven methods on {@code org.jmouse.jdbc.dialect.Dialect}, and nowhere else. The compiler asks
 * for a <em>string</em> — how this database quotes a name, how it reads text as a number, how it shifts a
 * moment — and assembles the statement out of what it is handed. Grep the two modules and the whole of
 * the difference is thirteen call sites.</p>
 *
 * <p>⚠️ Only the MySQL statements are run: {@code jmq_demo} exists on MySQL and there is no PostgreSQL
 * copy of it. What is being shown is the <strong>translation</strong>, and it is worth reading with the
 * two lines next to each other — half of these differences are not syntax, they are behaviour.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class DialectDemo {

    private DialectDemo() {
    }

    public static void main(String[] arguments) {
        run();
        Demo.summary();
    }

    public static void run() {
        scenario("14 · ОДИН ЗАПИТ, ДВА ДІАЛЕКТИ — де саме різниця");

        QueryEngine mysql    = engine();
        QueryEngine postgres = mysql.forDialect(new PostgreSqlDialect());

        say("QueryEngine.forDialect(other) — та сама конфігурація, наведена на іншу базу.");
        say("Нижче кожен випадок: jMQ, потім те, у що він компілюється в кожній.");

        compare(mysql, postgres, "quote — як цитується ім'я",
                "builds", "build[branch] == 'master'");

        compare(mysql, postgres, "⚠️ textAsInteger — не синтаксис, а ПОВЕДІНКА",
                "students", "student[grade] | int < 60");
        say("     MySQL:      CAST('n/a' AS SIGNED) = 0 з попередженням, яке ніхто не читає");
        say("     PostgreSQL: те саме CAST кидає помилку і вбиває запит для ВСІХ рядків");
        say("     ⚠️ Тому обидва шаблони — guarded CASE WHEN: без нього дві бази відповідають");
        say("        на той самий запит по-різному. Це та причина, чому там взагалі CASE.");

        compare(mysql, postgres, "textAsDecimal — SIGNED/DECIMAL проти BIGINT/NUMERIC",
                "catalog", "product[price] | bigDecimal > 700");

        compare(mysql, postgres, "⚠️ shift — інтервал пишеться зовсім інакше",
                "builds", "build[at] > now() - hours(24)");

        compare(mysql, postgres, "caseInsensitiveLike — тут навмисно ОДНАКОВО",
                "candidates", "candidate[role] is contains('go')");
        say("     ⚠️ PostgreSQL має власний ILIKE, і він читабельніший — але його немає в MySQL.");
        say("        Сенс пари в тому, що той самий запит означає те саме; різниця лишається");
        say("        тільки там, де вона неминуча.");

        compare(mysql, postgres, "before/after — SUBSTRING_INDEX проти split_part",
                "catalog", "product[price] | before('.') | int > 700");

        section("⚠️ І ЦІЛА В'ЮХА — join, проєкція, сортування");
        String view = """
                view "Кардіологія" on appointments {
                  where   visit[doctor].speciality == 'cardiology' and visit[date] > now() - days(7)
                  columns visit[patient] as patient, visit[doctor].name as doctor
                  order   visit[date] desc
                }
                """;

        say("");
        view.strip().lines().forEach(line -> say("     " + line));
        say("");
        say("  MySQL      " + mysql.compileDocument(view).sql());
        say("  PostgreSQL " + postgres.compileDocument(view).sql());

        section("⚠️ А ЩО НЕ ЗАЛЕЖИТЬ ВІД БАЗИ ЗОВСІМ");
        Fragment fromMySql    = mysql.compileFilter("builds", "build[at] > now() - hours(24)");
        Fragment fromPostgres = postgres.compileFilter("builds", "build[at] > now() - hours(24)");

        note(fromMySql.parameters().size() == fromPostgres.parameters().size(),
                "однакова кількість параметрів: " + fromMySql.parameters().size());
        note(!fromMySql.sql().equals(fromPostgres.sql()),
                "і різний текст — тобто вся різниця осіла в SQL, а не в тому, що зв'язується");
        say("     ⚠️ Значення ніколи не потрапляють у текст, тож діалект не може на них вплинути.");
        say("        Саме тому перемикання бази не може змінити ВІДПОВІДЬ — лише її написання.");
    }

    /** One expression, compiled by both engines, printed one under the other. */
    private static void compare(QueryEngine mysql, QueryEngine postgres, String what, String source, String jmq) {
        section(what);
        say("     jMQ        " + jmq);

        List<Map.Entry<String, QueryEngine>> engines = List.of(
                Map.entry("MySQL     ", mysql), Map.entry("PostgreSQL", postgres));

        for (Map.Entry<String, QueryEngine> named : engines) {
            Fragment compiled = named.getValue().compileFilter(source, jmq);

            say("     " + named.getKey() + " " + condition(compiled.sql()));
        }
    }

    /**
     * The {@code WHERE} on its own.
     *
     * <p>⚠️ The projection and the {@code FROM} are identical in both and would only make the line long
     * enough to hide the part that differs.</p>
     */
    private static String condition(String statement) {
        int where = statement.indexOf(" WHERE ");

        return where < 0 ? statement : statement.substring(where + " WHERE ".length());
    }
}
