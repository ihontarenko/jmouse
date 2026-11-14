package org.jmouse.cad.libre.pcb;

import org.jmouse.cad.table.ReadTable;
import org.jmouse.cad.table.TableSource;
import org.jmouse.cad.table.ShippedTables;
import org.jmouse.cad.document.DocumentKind;
import org.jmouse.cad.document.Provenance;
import org.jmouse.cad.exchange.imported.FilingTable;
import org.jmouse.cad.exchange.imported.ImportFiling;
import org.jmouse.cad.exchange.imported.SheetFiling;
import org.jmouse.libre.pcb.LibrePcbLibrary;

import java.util.Map;
import java.util.List;
import java.util.Optional;

/**
 * How a drawing converted from LibrePCB is filed, and what each of its layers is to us. A project carries the elements of the
 * libraries it was drawn from, each saying who drew it: that author is named on what is converted from it. The terms are its
 * library's, which a project does not carry, so none are claimed; a part fetched from an installed library is converted by a filing
 * {@linkplain #claiming claiming} that library's terms, where the table names them.
 *
 * @param sheet             what it says as every product's table does: category, tags, transient, mark symbols
 * @param ground            the net that is node 0
 * @param sheetGap          how far apart its sheets are laid, millimetres
 * @param sheetGrid         the grid a sheet's edge stays on, millimetres
 * @param lineSpacing       how far apart the lines of a board's words stand, as a share of their height
 * @param layers            what each LibrePCB layer is for, by LibrePCB's name
 * @param pcbRules          the way of making a pcb a board's design rules change
 * @param pcbRuleNames      which of a board's checks each of our rules is, by ours
 * @param classOpening      what a net class's identifier opens with, where its name is no identifier
 * @param installedLicences the terms each library LibrePCB publishes stands under, as an SPDX expression, by the library's UUID
 * @param terms             the terms everything converted is claimed under; null where none are known — a project's
 * @param library           the library what is converted comes from, what tells what each element is ({@link #kindsOf}); null
 *                          where none is known — then each is unfiled
 */
public record LibrePcbFiling(SheetFiling sheet, String ground, double sheetGap, double sheetGrid, double lineSpacing, Map<String, String> layers,
                             String pcbRules, Map<String, String> pcbRuleNames, String classOpening, Map<String, String> installedLicences,
                             String terms, LibrePcbLibrary library) implements ImportFiling {


    /** The table's address in the catalogue. */
    public static final String TABLE = "libre-pcb-libraries";

    /** Where its module ships it. */
    public static final ShippedTables SHIPPED = ShippedTables.in(LibrePcbFiling.class, "jmouse-cad-libre-pcb", "org/jmouse/cad/libre/pcb/tables");

    public LibrePcbFiling {
        layers = Map.copyOf(layers);
        pcbRuleNames = Map.copyOf(pcbRuleNames);
        installedLicences = Map.copyOf(installedLicences);
    }

    /** The table this library ships. */
    public static LibrePcbFiling shipped() {
        return readFrom(SHIPPED).current();
    }

    /**
     * As its source answers at each use — the engine's catalogue table, its curator's revision where one is kept —, read again
     * only when another revision answers.
     */
    public static ReadTable<LibrePcbFiling> readFrom(TableSource tables) {
        return new ReadTable<>(tables, TABLE, sheet -> of(FilingTable.of(TABLE, sheet)));
    }

    public static LibrePcbFiling of(FilingTable table) {
        return new LibrePcbFiling(SheetFiling.of(table), table.text("ground"), table.measure("sheet-gap") * 1000,
                table.measure("sheet-grid") * 1000, Double.parseDouble(table.text("text-line-spacing")), table.ours("libre-pcb"), table.text("pcb-rules"),
                table.assignments("pcb-rule-names"), table.text("pcb-class-opening"), table.assignments("installed-licences"), null, null);
    }

    @Override
    public Optional<String> codeOfImported(DocumentKind kind, String slug, Provenance provenance) {
        return LibrePcbNames.codeOfImported(kind, slug, provenance.source());
    }

    @Override
    public Optional<String> layerOf(String layer) {
        return Optional.ofNullable(layers.get(layer));
    }

    /** The terms an installed library stands under, by its UUID; empty where the table names none. */
    public Optional<String> licenceOf(String library) {
        return Optional.ofNullable(installedLicences.get(library));
    }

    /** The same filing, everything it converts claimed under {@code terms}: what an installed library's parts are fetched with. */
    public LibrePcbFiling claiming(String terms) {
        return new LibrePcbFiling(sheet, ground, sheetGap, sheetGrid, lineSpacing, layers, pcbRules, pcbRuleNames, classOpening, installedLicences,
                terms, library);
    }

    /** The same filing within the library what it converts comes from: what tells what each element is. */
    public LibrePcbFiling within(LibrePcbLibrary library) {
        return new LibrePcbFiling(sheet, ground, sheetGap, sheetGrid, lineSpacing, layers, pcbRules, pcbRuleNames, classOpening, installedLicences,
                terms, library);
    }

    /**
     * What LibrePCB says a converted element is, the most specific first: the categories the element its provenance names is
     * filed in, then their parents, as the library holds them — a 3D model by its package's. None without a library, or for an
     * element it does not hold.
     */
    @Override
    public List<String> kindsOf(Provenance provenance) {
        if (library == null) {
            return List.of();
        }
        return LibrePcbNames.elementOf(provenance.source()).flatMap(this::elementIdOf).map(library::categoriesOf).orElse(List.of());
    }

    /** The UUID of the element a source names, among the library's elements of its kind; a model's, its package's. */
    private Optional<String> elementIdOf(LibrePcbNames.SourcedElement element) {
        // A model says nothing of itself: its package does.
        if (element.kind() == LibrePcbNames.SourceKind.MODEL) {
            return library.packages().values().stream().filter(found -> found.modelNames().keySet().stream()
                    .anyMatch(model -> model.startsWith(element.idStart()))).map(LibrePcbLibrary.Package::id).findFirst();
        }
        Map<String, ?> ofKind = switch (element.kind()) {
            case SYMBOL -> library.symbols();
            case COMPONENT -> library.components();
            case PACKAGE, MODEL -> library.packages();
        };
        return ofKind.keySet().stream().filter(id -> id.startsWith(element.idStart())).findFirst();
    }

    /** A converted drawing: its source said, no holder, and the terms this filing claims — none for a project's. */
    @Override
    public Provenance provenance(String source, boolean installed) {
        return new Provenance(source, null, terms);
    }

    /** A converted element of a library: its source, the author it names, and the terms this filing claims — none for a project's. */
    public Provenance provenance(String source, String author) {
        return new Provenance(source, author == null || author.isBlank() ? null : author.strip(), terms);
    }
}
