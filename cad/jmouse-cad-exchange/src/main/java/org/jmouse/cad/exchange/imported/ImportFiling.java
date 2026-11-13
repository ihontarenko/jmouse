package org.jmouse.cad.exchange.imported;

import org.jmouse.cad.document.DocumentKind;
import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.document.Provenance;
import org.jmouse.cad.simulation.CategoryDocument;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * How documents converted from another product's files are filed, as that product's table says it: what every table
 * says alike ({@link SheetFiling}) — the library's category and the tags, the transient, the mark symbols —, our layer each of its
 * layers is, and whose a converted drawing is. KiCad's table is one; Eagle's and Altium's others.
 */
public interface ImportFiling {

    /** The word a library's category is named with, after the library's name. */
    String LIBRARY = "library";

    /** What the table says as every product's does. */
    SheetFiling sheet();

    /** The category a symbol, a footprint, a 3D model or a component an import brings is filed in: the other program's own. */
    default String libraryCategory() {
        return sheet().libraryCategory();
    }

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
     * The category a document of one of the program's libraries is filed in: that library's own, standing under the
     * program's ({@link #libraryCategory()}) — {@code kicad-capacitor-tht-library} —; the program's itself for one of no
     * library. Its slug ends in the word of what it is, as a converted footprint's ends in {@code footprint}: a library
     * named as its one part — {@code AD5292BRUZ-100-RL7} — would otherwise take that part's address.
     */
    default String libraryCategory(Provenance provenance) {
        return libraryOf(provenance).filter(library -> !library.isBlank())
                .map(library -> ImportedNames.slug(libraryCategory(), library, LIBRARY)).orElse(libraryCategory());
    }

    /**
     * The category document of the library a converted drawing comes from: called as the program calls the library,
     * standing under the program's — what goes with the drawing, so it is filed in a category that exists. None for a
     * drawing of no library.
     */
    default Optional<DocumentNode> libraryCategoryDocument(Provenance provenance) {
        return libraryOf(provenance).filter(library -> !library.isBlank())
                .map(library -> new CategoryDocument(libraryCategory(provenance), library.strip(), null, libraryCategory(), null).document());
    }

    /**
     * Converted documents with the categories of their libraries before them, each once — what a store writes in order,
     * so a document's category stands before the document; one already among the documents is not written twice.
     */
    default List<DocumentNode> withLibraryCategories(List<DocumentNode> documents) {
        Set<String> present = documents.stream().map(DocumentNode::slug).collect(Collectors.toSet());
        Map<String, DocumentNode> categories = new LinkedHashMap<>();
        for (DocumentNode document : documents) {
            libraryCategoryDocument(Provenance.of(document)).filter(category -> !present.contains(category.slug()))
                    .ifPresent(category -> categories.putIfAbsent(category.slug(), category));
        }
        if (categories.isEmpty()) {
            return documents;
        }
        List<DocumentNode> filed = new ArrayList<>(categories.values());
        filed.addAll(documents);
        return filed;
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
