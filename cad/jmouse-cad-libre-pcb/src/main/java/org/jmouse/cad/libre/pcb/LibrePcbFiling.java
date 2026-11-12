package org.jmouse.cad.libre.pcb;

import org.jmouse.cad.document.DocumentKind;
import org.jmouse.cad.document.Provenance;
import org.jmouse.cad.exchange.imported.FilingTable;
import org.jmouse.cad.exchange.imported.ImportFiling;
import org.jmouse.cad.exchange.imported.SheetFiling;

import java.util.Map;
import java.util.Optional;

/**
 * How a drawing converted from a LibrePCB project is filed, and what each of its layers is to us. A project carries the
 * elements of the libraries it was drawn from, each saying who drew it: that author is named on what is converted from it.
 * The terms are its library's, which a project does not carry, so none are claimed.
 *
 * @param sheet        what it says as every product's table does: category, tags, transient, mark symbols
 * @param ground       the net that is node 0
 * @param sheetGap     how far apart its sheets are laid, millimetres
 * @param sheetGrid    the grid a sheet's edge stays on, millimetres
 * @param lineSpacing  how far apart the lines of a board's words stand, as a share of their height
 * @param layers       what each LibrePCB layer is for, by LibrePCB's name
 * @param pcbRules     the way of making a pcb a board's design rules change
 * @param pcbRuleNames which of a board's checks each of our rules is, by ours
 * @param classOpening what a net class's identifier opens with, where its name is no identifier
 */
public record LibrePcbFiling(SheetFiling sheet, String ground, double sheetGap, double sheetGrid, double lineSpacing, Map<String, String> layers,
                             String pcbRules, Map<String, String> pcbRuleNames, String classOpening) implements ImportFiling {

    private static final String RESOURCE = "libre-pcb-libraries.sht";

    public LibrePcbFiling {
        layers = Map.copyOf(layers);
        pcbRuleNames = Map.copyOf(pcbRuleNames);
    }

    /** The table this library ships. */
    public static LibrePcbFiling standard() {
        return of(FilingTable.shipped(LibrePcbFiling.class, RESOURCE));
    }

    public static LibrePcbFiling of(FilingTable table) {
        return new LibrePcbFiling(SheetFiling.of(table), table.text("ground"), table.measure("sheet-gap") * 1000,
                table.measure("sheet-grid") * 1000, Double.parseDouble(table.text("text-line-spacing")), table.layers("libre-pcb"), table.text("pcb-rules"),
                table.assignments("pcb-rule-names"), table.text("pcb-class-opening"));
    }

    @Override
    public Optional<String> codeOfImported(DocumentKind kind, String slug, Provenance provenance) {
        return LibrePcbNames.codeOfImported(kind, slug, provenance.source());
    }

    @Override
    public Optional<String> layerOf(String layer) {
        return Optional.ofNullable(layers.get(layer));
    }

    /** A converted drawing is the project's: its source said, no holder and no terms claimed. */
    @Override
    public Provenance provenance(String source, boolean installed) {
        return new Provenance(source, null, null);
    }

    /** A converted element of a library the project carries: its source, and the author it names; no terms claimed. */
    public Provenance provenance(String source, String author) {
        return new Provenance(source, author == null || author.isBlank() ? null : author.strip(), null);
    }
}
