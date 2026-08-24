package org.jmouse.query.sql.mapping;

import org.jmouse.query.schema.QueryAttribute;
import org.jmouse.query.sql.AttributeMapping;
import org.jmouse.query.sql.Fragment;
import org.jmouse.query.sql.SqlContext;

/**
 * An attribute that lives as a row in a bag — reached by a join, one alias apiece.
 *
 * <p>A product supplies a {@link BagTable} and gets the join for free:</p>
 *
 * <pre>{@code
 * BagMapping.of(new BagTable("field_entries", "entry_id", "field", "text_value"))
 * }</pre>
 *
 * <h2>⚠️ One alias per attribute, and it is not an optimisation</h2>
 *
 * <p>One row of a bag cannot be both the name and the quantity. Two attributes sharing a join alias
 * therefore ask a single row to equal two different things, and the query returns nothing where an
 * {@code and} was meant — while looking entirely correct on data small enough to check by eye. The alias
 * is keyed on the attribute's name, so mentioning one attribute three times costs one join and mixing
 * two costs two.</p>
 *
 * <h2>⚠️ The value comes back RAW</h2>
 *
 * <p>A bag column holds text. Making it a number is the converter's job — {@code | int} — and refusing
 * to compare it without one is the checker's. Doing either here would put one decision in two places,
 * and the two would eventually disagree.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class BagMapping implements AttributeMapping {

    private final BagTable table;

    public BagMapping(BagTable table) {
        this.table = table;
    }

    public static BagMapping of(BagTable table) {
        return new BagMapping(table);
    }

    @Override
    public Fragment expression(QueryAttribute attribute, SqlContext context) {
        String alias = context.alias(attribute.name());

        // ⚠️ The row's own key unless the product said otherwise. An asset's values live on the ENTRY it
        // describes, so its bag correlates `field_entries.form_entry_id = assets.form_entry_id` — one hop
        // sideways. Assuming the key there would compile and return rows about something else.
        String local = table.localColumn() == null
                ? context.rootKey()
                : context.column(table.localColumn());

        context.join(alias, Join.left(table.table(), alias)
                .on(alias, table.foreignKey()).equalTo(local)
                // ⚠️ `source`, not `name`: what the STORE calls this attribute, which the schema recorded
                // when it built the path. Recovering it by taking the written path apart — the previous
                // shape — made every product re-implement the language's own spelling rules.
                .and(alias, table.keyColumn()).equalToValue(attribute.source())
                .toFragment(context.dialect()));

        return Fragment.of(context.column(alias, table.valueColumn()));
    }
}
