package org.jmouse.query.sql.smoke.demo;

import org.jmouse.query.translate.row.RowTranslator;
import org.jmouse.query.sql.QueryEngine;
import org.jmouse.query.sql.QuerySource;

import java.util.Map;

import static org.jmouse.query.sql.smoke.demo.Demo.configuration;
import static org.jmouse.query.sql.smoke.demo.Demo.engine;
import static org.jmouse.query.sql.smoke.demo.Demo.note;
import static org.jmouse.query.sql.smoke.demo.Demo.query;
import static org.jmouse.query.sql.smoke.demo.Demo.refuse;
import static org.jmouse.query.sql.smoke.demo.Demo.result;
import static org.jmouse.query.sql.smoke.demo.Demo.say;
import static org.jmouse.query.sql.smoke.demo.Demo.scenario;
import static org.jmouse.query.sql.smoke.demo.Demo.section;

/**
 * The three things a real board filter needs beyond a column and a bag.
 *
 * <pre>
 *   mvn -pl jmouse-query-sql exec:java -Dexec.mainClass=org.jmouse.query.sql.smoke.demo.VocabularyDemo
 * </pre>
 *
 * <p>Written from what Tessera's saved filters actually say. A flat one — {@code issue.points > 4 and
 * issue.status == 'status-done'} — compiled through jMQ already; these three did not, and each was a
 * different kind of missing:</p>
 *
 * <table>
 *   <caption>What a stored predicate writes, and what it needed</caption>
 *   <tr><th>{@code issue.status.category == 'DONE'}</th><td>a value one hop away — {@code join}</td></tr>
 *   <tr><th>{@code issue.assignee == currentMember}</th><td>a value the CALLER supplies, bound</td></tr>
 *   <tr><th>{@code issue.labels is hasAny([…])}</th><td>many rows per row — {@code collection}</td></tr>
 * </table>
 *
 * <p>⚠️ Demonstrated on the clinic and the ATS rather than on Tessera, because these are library
 * mechanisms and a demonstration tied to one product's tables would read as a feature of that product.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class VocabularyDemo {

    private VocabularyDemo() {
    }

    public static void main(String[] arguments) {
        run();
        Demo.summary();
    }

    public static void run() {
        QueryEngine engine = engine();

        // ── 1 · a row one hop away ────────────────────────────────────────────────────────────────
        scenario("11 · ОДИН ХОП — значення, що лежить у сусідній таблиці");
        configuration("appointments");

        String bySpeciality = """
                view "Кардіологія цього тижня" on appointments {
                  where   visit[doctor].speciality == 'cardiology' and visit[status] == 'scheduled'
                  columns visit[patient] as patient, visit[doctor].name as doctor,
                          visit[doctor].room as room, visit[date] as at
                  order   visit[date] asc
                }
                """;

        query("три атрибути з doctors — і ОДИН join", bySpeciality);
        result(engine.compileDocument(bySpeciality));
        say("   ⚠️ подивись на SQL: `LEFT JOIN doctors j1` рівно один раз. Аліас ключується на ТАБЛИЦІ,");
        say("      бо name, room і speciality — це три колонки одного рядка. У мішка правило обернене:");
        say("      там кожен атрибут — окремий рядок, і спільний аліас питав би один рядок бути двома.");

        section("⚠️ LEFT, не INNER — рядок без сусіда має вижити");
        String orphans = """
                view "Усі прийоми, з лікарем або без" on appointments {
                  columns visit[patient] as patient, visit[doctor].name as doctor
                  order   visit[patient] asc
                }
                """;

        result(engine.compileDocument(orphans), 3);

        section("⚠️ ВІДМОВИ");
        refuse("колонка, якої в тій таблиці немає — ловиться схемою, не базою",
                () -> engine.compileFilter("appointments", "visit[doctor].salary > 0"));

        // ── 2 · a value the caller supplies ───────────────────────────────────────────────────────
        scenario("12 · ЗНАЧЕННЯ ВІД ВИКЛИКАЧА — currentMember, який запит не може вигадати");

        String mine = "visit[doctor] == currentDoctor and visit[status] == 'scheduled'";

        query("збережений фільтр — текст той самий для кожного, хто його відкриє", mine);
        result(engine.compileFilter("appointments", mine, Map.of("currentDoctor", "doctor-lysenko")));

        say("");
        say("   той самий текст, інша людина:");
        result(engine.compileFilter("appointments", mine, Map.of("currentDoctor", "doctor-hrytsenko")));

        section("⚠️ ЗВ'ЯЗАНЕ, А НЕ ВКЛЕЄНЕ");
        String hostile = "doctor-lysenko' OR '1'='1";

        note(!engine.compileFilter("appointments", mine, Map.of("currentDoctor", hostile)).sql().contains("OR '1'"),
                "ім'я з лапкою і диз'юнкцією не змінює жодного символу SQL — воно параметр");
        say("   " + engine.compileFilter("appointments", mine, Map.of("currentDoctor", hostile)).parameters());

        section("⚠️ ВІДМОВИ");
        refuse("ім'я, якого ніхто не оголосив — ні атрибут, ні значення",
                () -> engine.compileFilter("appointments", "visit[doctor] == currentDoctor"));
        refuse("ім'я, яке водночас атрибут і значення — неможливо сказати, що мав на увазі запит",
                () -> engine.compileFilter("appointments", "visit[status] == 'x'",
                        Map.of("visit[status]", "scheduled")));

        // ── 3 · many rows per row ─────────────────────────────────────────────────────────────────
        scenario("13 · КОЛЕКЦІЯ — багато рядків на рядок, і жодного «того самого» значення");
        configuration("applicants");

        String anySkill = """
                view "Го або Kubernetes" on applicants {
                  where   candidate[skills] is hasAny(['kubernetes', 'terraform'])
                  columns candidate[name] as name, candidate[role] as role, candidate[level] as level
                  order   candidate[name] asc
                }
                """;

        query("is hasAny([…]) — хоч одне з переліченого", anySkill);
        result(engine.compileDocument(anySkill));
        say("   ⚠️ EXISTS, а не join — інакше кандидат із трьома навичками повернувся б ТРИЧІ, і");
        say("      кожен count по цьому результату був би тихо неправильним.");

        String allSkills = """
                view "І Go, і Kubernetes" on applicants {
                  where   candidate[skills] is hasAll(['go', 'kubernetes'])
                  columns candidate[name] as name, candidate[level] as level
                  order   candidate[name] asc
                }
                """;

        query("is hasAll([…]) — усе перелічене, рахуючи DISTINCT", allSkills);
        result(engine.compileDocument(allSkills));

        String noneSkills = """
                view "Без Kubernetes" on applicants {
                  where   candidate[skills] is hasNone(['kubernetes'])
                  columns candidate[name] as name, candidate[role] as role
                  order   candidate[name] asc
                }
                """;

        query("is hasNone([…]) — NOT EXISTS", noneSkills);
        result(engine.compileDocument(noneSkills));

        section("⚠️ І ВСЕ ТРИ РАЗОМ — так виглядає справжній фільтр борди");
        String together = """
                view "Мої сеньйори на Go" on applicants {
                  where   candidate[skills] is hasAll(['go'])
                          and candidate[level] in ['senior', 'staff']
                          and candidate[status] == reviewing
                          and candidate[applied] > now() - days(30)
                  columns candidate[name] as name, candidate[role] as role, candidate[applied] as applied
                  order   candidate[applied] desc
                }
                """;

        query("колекція + значення від викликача + годинник", together);
        result(engine.compileDocument(together, Map.of("reviewing", "new")));

        section("⚠️ `is not hasAny(…)` — заперечення, яке ЛЕГКО загубити");
        String negated = "candidate[skills] is not hasAny(['kubernetes'])";

        say("     jMQ  " + negated);
        say("     SQL  " + engine.compileFilter("applicants", negated).sql());
        note(engine.compileFilter("applicants", negated).sql().contains("NOT ("),
                "NOT на місці — і це не дрібниця: без нього запит відповідав би РІВНО НАВПАКИ");
        say("     ⚠️ Саме так воно й було, поки питання про колекцію поверталося з visitTest раніше,");
        say("        ніж рядок, що загортає заперечений тест. Тепер вихід один, і гілка не може");
        say("        відповісти протилежне тому, що її спитали.");

        section("⚠️ ВІДМОВИ — колекція не є значенням");
        refuse("порівняння колекції з одним значенням",
                () -> engine.compileFilter("applicants", "candidate[skills] == 'go'"));
        refuse("сортування за колекцією",
                () -> engine.compileDocument("""
                        view "v" on applicants { columns candidate[name] as n order candidate[skills] asc }
                        """));
        refuse("порожній перелік — питання ні про що",
                () -> engine.compileFilter("applicants", "candidate[skills] is hasAny([])"));

        section("⚠️ А ЩО КАЖЕ ДРУГИЙ БЕКЕНД");
        QuerySource applicants = engine.source("applicants").orElseThrow();

        say("   memory declares: " + new RowTranslator(applicants.schema()).capabilities().translator()
            + ", join=" + new RowTranslator(applicants.schema()).capabilities()
                    .has(org.jmouse.el.translate.Capability.JOIN));
        say("   ⚠️ тобто in-memory прев'ю чесно відмовиться від того, чого не вміє, замість повернути");
        say("      правдоподібне. Це і є те, заради чого Capabilities оголошуються, а не вгадуються.");
    }
}
