package org.jmouse.query.sql.smoke;

import org.jmouse.jdbc.dialect.MySqlDialect;
import org.jmouse.query.el.QueryLanguage;
import org.jmouse.query.sql.QueryEngine;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.jmouse.query.sql.smoke.Smokes.INNOVENTA;
import static org.jmouse.query.sql.smoke.Smokes.TESSERA;
import static org.jmouse.query.sql.smoke.Smokes.banner;
import static org.jmouse.query.sql.smoke.Smokes.execute;
import static org.jmouse.query.sql.smoke.Smokes.jmq;
import static org.jmouse.query.sql.smoke.Smokes.note;
import static org.jmouse.query.sql.smoke.Smokes.refuse;
import static org.jmouse.query.sql.smoke.Smokes.summary;

/**
 * The same two products as {@link QuerySmoke}, configured from a <strong>file</strong> instead of Java.
 *
 * <pre>
 *   mvn -pl jmouse-query-sql exec:java -Dexec.mainClass=org.jmouse.query.sql.smoke.DeclaredSmoke
 * </pre>
 *
 * <p>⚠️ <strong>The point is that nothing else changes.</strong> The engine, the checker, the compiler
 * and the SQL are identical to the Java-configured run — a {@code source { }} block produces the same
 * {@code QueryTarget}, {@code QuerySchema} and {@code AttributeMapping} a product hands over in code. A
 * declarative layer that produced a different <em>kind</em> of thing would have been a second mechanism
 * rather than a second spelling.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class DeclaredSmoke {

    public static void main(String[] arguments) throws IOException {
        String innoventaSources = read("jmq/innoventa.jmq");
        String tesseraSources = read("jmq/tessera.jmq");

        QueryEngine invt = QueryEngine.with(new MySqlDialect()).sources(innoventaSources).build();
        QueryEngine tssr = QueryEngine.with(new MySqlDialect()).sources(tesseraSources).build();

        banner("1 · WHAT THE FILE SAYS");
        System.out.println(tesseraSources.strip().indent(5));

        banner("2 · AND WHAT IT NOW ANSWERS — nothing was written in Java");
        run(TESSERA, tssr, """
                view "Велике й закрите" on issues {
                  where   issue.points > 4 and issue.status == 'status-done'
                  columns issue.key as k, issue.summary as title, issue.points as pts
                  order   issue.points desc, issue.key asc
                }
                """);

        banner("3 · THE BAG, DECLARED THE SAME WAY — two hops, ids in the key column");
        run(INNOVENTA, invt, """
                view "Опір понад 2k" on inventory {
                  where   entry[resistance] | before("|") | int > 2000
                  columns entry[name] as name, entry[resistance] as raw,
                          entry[resistance] | before("|") as ohms,
                          entry[resistance] | after("|")  as unit
                }
                """);

        banner("4 · A SECOND SOURCE OVER THE SAME TABLE — the numeric column instead");
        run(INNOVENTA, invt, """
                view "Кількість" on numbers {
                  where   entry[quantity] | int > 100
                  columns entry[quantity] as qty
                }
                """);

        banner("5 · A THIRD SUBJECT AREA, SAME FILE");
        run(INNOVENTA, invt, """
                view "Прилади Keithley" on assets {
                  where   asset[manufacturer] is contains('keith')
                  columns asset[manufacturer] as maker, asset[model] as model, asset[serial] as serial
                }
                """);

        banner("6 · A FUNCTION LIBRARY IN ITS OWN DOCUMENT, USED BY A VIEW");
        run(TESSERA, tssr, """
                function big(threshold : 5) { where issue.points > threshold }
                function unassigned()       { where issue.assignee is null }

                view "Велике й нічиє" on issues {
                  where   big(13) and unassigned()
                  columns issue.key as k, issue.points as pts
                  order   issue.points desc
                }
                """);

        banner("⚠️ 7 · THE FILE ROUND-TRIPS — written back out and re-read identically");
        QueryLanguage language = new QueryLanguage();
        String once = language.rewrite(tesseraSources);
        String twice = language.rewrite(once);

        System.out.println(once.indent(5));
        note(once.equals(twice), "rewriting an already-rewritten file changes nothing  ok");

        banner("⚠️ 8 · A DECLARATION THAT SAYS TOO LITTLE IS REFUSED BY NAME");
        refuse(() -> QueryEngine.with(new MySqlDialect())
                .sources("source broken { attribute a from b text in column }")
                .build());
        refuse(() -> QueryEngine.with(new MySqlDialect())
                .sources("source broken { from t as x key id attribute a from b colour in column }")
                .build());
        refuse(() -> QueryEngine.with(new MySqlDialect())
                .sources("source broken { from t as x key id attribute a from b text in bag }")
                .build()
                .compileFilter("broken", "a == 'x'"));

        summary();
    }

    private static void run(String url, QueryEngine engine, String source) {
        jmq(source);

        try {
            execute(url, engine.compileDocument(source), 4);
        } catch (Exception exception) {
            note(false, "<<< " + exception.getMessage());
        }
    }

    private static String read(String resource) throws IOException {
        try (InputStream stream = DeclaredSmoke.class.getClassLoader().getResourceAsStream(resource)) {
            if (stream == null) {
                throw new IOException("'%s' is not on the classpath".formatted(resource));
            }

            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
