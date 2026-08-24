package org.jmouse.query.sql.smoke.demo;

import org.jmouse.query.sql.QueryEngine;

import static org.jmouse.query.sql.smoke.demo.Demo.configuration;
import static org.jmouse.query.sql.smoke.demo.Demo.engine;
import static org.jmouse.query.sql.smoke.demo.Demo.query;
import static org.jmouse.query.sql.smoke.demo.Demo.refuse;
import static org.jmouse.query.sql.smoke.demo.Demo.result;
import static org.jmouse.query.sql.smoke.demo.Demo.say;
import static org.jmouse.query.sql.smoke.demo.Demo.scenario;
import static org.jmouse.query.sql.smoke.demo.Demo.section;

/**
 * 7 · Веб-шоп — <strong>an export definition</strong>. A projection that computes, not a column list.
 *
 * <pre>
 * view "Прайс з націнкою" on catalog {
 *   columns sku, name,
 *           product[price] | bigDecimal as base,
 *           product[price] | bigDecimal * 1.2 as retail
 *   order   name asc
 * }
 * </pre>
 *
 * <h2>⚠️ This is the one schemaless example, and it is the shape that is hard</h2>
 *
 * <p>{@code product[price]} is not a column: it is a row in {@code product_attributes} keyed by a name.
 * Each attribute a query touches becomes its <strong>own</strong> {@code LEFT JOIN} with its own alias —
 * {@code j1} for the price, {@code j2} for the colour. Collapsing two into one join looks right on small
 * data and silently returns nothing, because one row of a bag cannot be both the price and the colour.</p>
 *
 * <p>⚠️ And {@code LEFT}, not {@code INNER}: a product with no colour must still appear in a price list,
 * with an empty cell. An inner join would drop the row entirely — a projection quietly deciding which
 * rows exist.</p>
 *
 * <h2>⚠️ {@code | bigDecimal} is not decoration here</h2>
 *
 * <p>The bag holds {@code '890'} as text. Without the converter {@code 1180 > 890} is a comparison of
 * words, and {@code "1180" > "890"} is <em>false</em> — the answer is wrong on every row and nothing
 * says so. The declaration says {@code unknown} precisely so that this is refused instead.</p>
 *
 * <h2>⚠️ A computed column binds BEFORE the conditions</h2>
 *
 * <p>{@code price * ? as retail} sits in the {@code SELECT}, which is written first — so its value binds
 * ahead of every join and every {@code WHERE}. Assembling a statement by hand is exactly where that gets
 * reversed, and the parameters then land in the wrong placeholders with no error.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class ShopDemo {

    private ShopDemo() {
    }

    public static void main(String[] arguments) {
        run();
        Demo.summary();
    }

    public static void run() {
        scenario("7 · ВЕБ-ШОП — визначення експорту: проєкція з обчисленням");
        configuration("catalog");

        QueryEngine engine = engine();

        String priceList = """
                view "Прайс з націнкою" on catalog {
                  columns sku, name,
                          product[price] | bigDecimal as base,
                          product[price] | bigDecimal * 1.2 as retail
                  order   name asc
                }
                """;

        query("the export, as it is stored", priceList);
        result(engine.compileDocument(priceList));

        String live = """
                view "Прайс, тільки живі позиції" on catalog {
                  where   product[price] | bigDecimal > 700
                  columns sku, name, product[colour] as colour,
                          product[price] | bigDecimal as base,
                          product[price] | bigDecimal * 1.2 as retail
                  order   product[price] | bigDecimal desc
                }
                """;

        query("⚠️ two bag attributes at once — price and colour, each its own join", live);
        result(engine.compileDocument(live));
        say("   ⚠️ подивись на SQL вище: два окремих LEFT JOIN, j1 і j2. Один join на обидва атрибути");
        say("      виглядав би правильно і тихо повертав би нуль рядків — бо один рядок мішка не може");
        say("      бути водночас ціною і кольором.");

        section("⚠️ THE COMPARISON THAT IS REFUSED, AND WHY IT MATTERS");
        refuse("product[price] > 700 — over a bag nobody promised anything about",
                () -> engine.compileDocument("""
                        view "v" on catalog { where product[price] > 700 columns sku }
                        """));
        say("       без конвертера це порівняння СЛІВ: \"1180\" < \"890\", бо \"1\" < \"8\".");
        say("       Відмова тут — єдине, що стоїть між експортом і тихо неправильним прайсом.");

        section("⚠️ ABSENCE — a row with no such attribute at all");
        String archived = """
                view "Архівні" on catalog {
                  where   product[archived] == 'yes'
                  columns sku, name, product[archived] as archived
                  order   sku asc
                }
                """;

        query("the two products that carry an `archived` attribute", archived);
        result(engine.compileDocument(archived));
        say("   ⚠️ решта не мають такого рядка в мішку взагалі — і саме тому \"не архівні\" не можна");
        say("      написати як != 'yes': відсутність не дорівнює нічому, її треба питати окремо.");
    }
}
