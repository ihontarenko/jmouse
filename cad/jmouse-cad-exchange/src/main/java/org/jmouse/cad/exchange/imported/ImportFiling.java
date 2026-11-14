package org.jmouse.cad.exchange.imported;

import org.jmouse.cad.document.DocumentKind;
import org.jmouse.cad.document.Provenance;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * How documents converted from another product's files are filed, as that product's table says it: what every table
 * says alike ({@link SheetFiling}) — where a part is filed, by what it is, and the tags, the transient, the mark symbols —, our
 * layer each of its layers is, and whose a converted drawing is. KiCad's table is one; Eagle's, Altium's and LibrePCB's others.
 */
public interface ImportFiling {

    /** What the table says as every product's does. */
    SheetFiling sheet();


    /**
     * The library of the program's a converted drawing comes from, as its source says it — {@code Capacitor_THT} —; empty
     * where the program names none, or this filing reads none from a source. A program whose drawings say their library
     * overrides it.
     */
    default Optional<String> libraryOf(Provenance provenance) {
        return Optional.empty();
    }

    /**
     * The code this program's import names a document by, recovered from what the import wrote of it — its kind, its slug and
     * its source —: for a document imported before imports named what they brought by codes. Empty for one this program did
     * not bring, or one whose code cannot be recovered.
     */
    default Optional<String> codeOfImported(DocumentKind kind, String slug, Provenance provenance) {
        return Optional.empty();
    }

    /**
     * The product's words for what a converted drawing is, the most specific first — what its category is chosen by: the library it
     * comes from, where the product files its parts so ({@code Resistor_SMD}). A product that says more of its parts says it here.
     */
    default List<String> kindsOf(Provenance provenance) {
        return libraryOf(provenance).filter(library -> !library.isBlank()).stream().toList();
    }

    /** The category a converted symbol, footprint, 3D model or component is filed in: by what the product says it is. */
    default String categoryOf(Provenance provenance) {
        return sheet().categories().categoryOf(kindsOf(provenance));
    }

    default List<String> symbolTags() {
        return sheet().symbolTags();
    }

    default List<String> footprintTags() {
        return sheet().footprintTags();
    }

    default List<String> componentTags() {
        return sheet().componentTags();
    }

    default List<String> circuitTags() {
        return sheet().circuitTags();
    }

    /** How long an imported sheet's transient runs until its author says, seconds. */
    default double span() {
        return sheet().span();
    }

    /** The symbol each mark of a sheet is drawn with, by the mark: {@code ground} is {@code ground-symbol}. */
    default Map<String, String> markSymbols() {
        return sheet().markSymbols();
    }

    /** Our layer one of the product's layers is; empty for one that is not a part's. */
    Optional<String> layerOf(String layer);

    /**
     * Whose a converted drawing is: one of the product's installed libraries is that library's holder's, under its
     * licence; one of a design's own claims neither — its holder and terms are the design's.
     *
     * @param source    where it was taken from
     * @param installed whether it comes from the product's installed libraries
     */
    Provenance provenance(String source, boolean installed);
}
