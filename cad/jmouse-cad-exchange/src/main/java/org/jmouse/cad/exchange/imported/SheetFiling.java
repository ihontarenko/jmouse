package org.jmouse.cad.exchange.imported;

import java.util.List;
import java.util.Map;

/**
 * What every product's filing table says the same way: the category a converted design is filed in, the category the parts
 * of the product's library it brings are filed in, the tags each kind carries, how long an imported sheet's transient runs,
 * and the symbol each mark is drawn with — read from the table's {@code category}, {@code library-category},
 * {@code symbol-tags}, {@code footprint-tags}, {@code component-tags}, {@code circuit-tags}, {@code transient} and
 * {@code mark-symbols}. What only one product's table says is that product's.
 *
 * @param category        where a converted circuit or pcb is filed: among the reader's designs
 * @param libraryCategory where a symbol, a footprint, a 3D model or a component an import brings is filed: apart, as the
 *                        other program's, so whose work it is stays plain
 * @param span            how long an imported sheet's transient runs until its author says, seconds
 */
public record SheetFiling(String category, String libraryCategory, List<String> symbolTags, List<String> footprintTags, List<String> componentTags,
                          List<String> circuitTags, double span, Map<String, String> markSymbols) {

    public SheetFiling {
        symbolTags = List.copyOf(symbolTags);
        footprintTags = List.copyOf(footprintTags);
        componentTags = List.copyOf(componentTags);
        circuitTags = List.copyOf(circuitTags);
        markSymbols = Map.copyOf(markSymbols);
    }

    public static SheetFiling of(FilingTable table) {
        return new SheetFiling(table.text("category"), table.text("library-category"), table.words("symbol-tags"), table.words("footprint-tags"),
                table.words("component-tags"), table.words("circuit-tags"), table.measure("transient"),
                table.assignments("mark-symbols"));
    }
}
