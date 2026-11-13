package org.jmouse.cad.folder;

import org.jmouse.cad.document.DocumentOwner;

import java.util.List;
import java.util.Optional;

/**
 * 🗂️ How the library folder reads to a person who opens it by hand: every place named in words, never by an identifier —
 * {@code libraries/owner/projects/kicad-control-board/circuits/supply.circuit.jm},
 * {@code libraries/owner/symbols/op-amps/lm358.symbol.jm}, {@code catalogue/components/passive/resistor.component.jm}. One class
 * says both ways of it — where a document stands, and whose and where a path's document is —, so they cannot drift apart; the
 * store knows neither the words nor their order.
 */
public interface FolderLayout {

    /** The folder the shared catalogue's documents stand in. */
    String CATALOGUE = "catalogue";

    /**
     * What a document's place is made of.
     *
     * @param owner    whose it is; null for the shared catalogue's
     * @param kind     its kind's word: {@code circuit}
     * @param category the category it is filed in; null for none
     * @param slug     its slug
     */
    record Place(DocumentOwner owner, String kind, String category, String slug) {
    }

    /**
     * What a path says of the document standing at it, before its text is read.
     *
     * @param owner    whose it is; null for the shared catalogue's
     * @param category the category its folder says; null for one standing directly in its kind's
     */
    record Placed(DocumentOwner owner, String category) {
    }

    /** What separates the words of a path. */
    String SEPARATOR = "/";

    /** What a document's file name ends in. */
    String SUFFIX = ".jm";

    /** A document's file: {@code <slug>.<kind>.jm}. */
    static String fileOf(Place place) {
        return place.slug() + "." + place.kind() + SUFFIX;
    }

    /** Where a document stands in the folder of its kind within its owner's: in its category's, or directly there for none. */
    static String standing(String ownerFolder, String kindFolder, Place place) {
        return ownerFolder + SEPARATOR + kindFolder + (place.category() == null ? "" : SEPARATOR + place.category()) + SEPARATOR
               + fileOf(place);
    }

    /**
     * What the words from a kind's folder down to a file say: a document standing there, in the category its folder names — none
     * directly in the kind's —; empty for words too few to hold one.
     */
    static Optional<Placed> standingAt(DocumentOwner owner, List<String> fromKind) {
        if (fromKind.size() < 2) {
            return Optional.empty();
        }
        return Optional.of(new Placed(owner, fromKind.size() > 2 ? fromKind.get(fromKind.size() - 2) : null));
    }

    /** The path a document stands at, its words joined by {@code /}, ending in its file's name. */
    String pathOf(Place place);

    /**
     * What a path's words say of the document at it; empty for words that are no document's place — a stray file, a folder of
     * a document's own files.
     */
    Optional<Placed> placedAt(List<String> words);
}
