package org.jmouse.cad.packages.footprint;

import org.jmouse.sheet.Sheet;
import org.jmouse.sheet.SheetValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A footprint library worked out from package tables by land pattern rules: every table names its {@code form}, the
 * family of that form emits its footprints. What becomes of them — written into a library, compared with one — is the
 * caller's.
 */
public final class FootprintGeneration {

    private static final String FORM = "form";

    private static final List<FootprintFamily> FAMILIES = List.of(new ChipFootprints(), new SmallOutlineFootprints(),
            new DualInlineFootprints(), new HeaderFootprints(), new AxialFootprints(), new RadialFootprints(),
            new InlineFootprints(), new HoleFootprints(), new QuadFootprints());

    private FootprintGeneration() {
    }

    /** The form a table names, when it is a package table at all; the land pattern rules name none. */
    public static Optional<String> form(Sheet table) {
        return table.field(FORM).map(field -> field.value() instanceof SheetValue.Word(String word) ? word : field.value().written());
    }

    /** Every footprint the tables list, in the order of the tables and of their entries. */
    public static List<StandardFootprint> generate(LandPatternRules rules, List<Sheet> tables) {
        List<StandardFootprint> footprints = new ArrayList<>();
        for (Sheet table : tables) {
            String form = form(table).orElseThrow(() -> new IllegalArgumentException(
                    "The package table '" + table.slug() + "' names no form"));
            footprints.addAll(family(form).footprints(table, rules));
        }
        return footprints;
    }

    private static FootprintFamily family(String form) {
        return FAMILIES.stream().filter(family -> family.form().equals(form)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No footprint family draws the form '" + form + "'"));
    }
}
