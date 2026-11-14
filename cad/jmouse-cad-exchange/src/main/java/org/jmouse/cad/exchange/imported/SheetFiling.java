package org.jmouse.cad.exchange.imported;

import java.util.List;
import java.util.Map;

/**
 * What every product's filing table says the same way: where the parts it brings are filed — by what they are, its words for
 * their kinds matched against our categories ({@link SourceCategories}) —, the tags each kind carries, how long an imported
 * sheet's transient runs, and the symbol each mark is drawn with — read from the table's {@code sources} entries,
 * {@code unfiled-category},
 * {@code symbol-tags}, {@code footprint-tags}, {@code component-tags}, {@code circuit-tags}, {@code transient} and
 * {@code mark-symbols}. What only one product's table says is that product's.
 *
 * <p>A converted circuit or pcb is filed in no category: it is the project's it is imported into, and the project is its
 * place.</p>
 *
 * @param categories where a symbol, a footprint, a 3D model or a component an import brings is filed: by what it is, among the
 *                   parts of every source; whose work it is stays in its provenance
 * @param span            how long an imported sheet's transient runs until its author says, seconds
 */
public record SheetFiling(SourceCategories categories, List<String> symbolTags, List<String> footprintTags, List<String> componentTags,
                          List<String> circuitTags, double span, Map<String, String> markSymbols) {

    public SheetFiling {
        symbolTags = List.copyOf(symbolTags);
        footprintTags = List.copyOf(footprintTags);
        componentTags = List.copyOf(componentTags);
        circuitTags = List.copyOf(circuitTags);
        markSymbols = Map.copyOf(markSymbols);
    }

    public static SheetFiling of(FilingTable table) {
        return new SheetFiling(SourceCategories.of(table), table.words("symbol-tags"), table.words("footprint-tags"),
                table.words("component-tags"), table.words("circuit-tags"), table.measure("transient"),
                table.assignments("mark-symbols"));
    }
}
