package org.jmouse.cad.libre.pcb;

import org.jmouse.cad.document.DocumentKind;
import org.jmouse.cad.document.TechnicalClass;
import org.jmouse.cad.exchange.imported.ImportedNames;
import org.jmouse.cad.mesh.ModelModule;
import org.jmouse.cad.pcb.PcbModule;
import org.jmouse.cad.schematic.SchematicModule;
import org.jmouse.libre.pcb.LibrePcbLibrary;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * What a converted drawing is called. A project's library may hold two elements of one name, so a slug carries the start of
 * the element's UUID as well, which also keeps it the same from one import to the next.
 */
final class LibrePcbNames {

    private static final String FORMAT = "librepcb";
    private static final int ID_KEPT = 8;

    private LibrePcbNames() {
    }

    static String symbol(LibrePcbLibrary.Symbol symbol) {
        return ImportedNames.slug(FORMAT, symbol.name(), kept(symbol.id()), "symbol");
    }

    static String footprint(LibrePcbLibrary.Package found, LibrePcbLibrary.Footprint footprint) {
        return ImportedNames.slug(FORMAT, found.name(), kept(footprint.id()), "footprint");
    }

    static String model(LibrePcbLibrary.Package found, String modelId) {
        return ImportedNames.slug(FORMAT, found.name(), kept(modelId), "model");
    }

    /**
     * The codes converted elements are named by, and named by in every reference: a project's library does not say which
     * library an element came from, and may hold two of one name, so a code is LibrePCB's and carries the start of the
     * element's UUID — {@code FP.LIBREPCB:R0805-06786894} —, the same from one import to the next.
     */
    static String symbolCode(LibrePcbLibrary.Symbol symbol) {
        return ImportedNames.code(TechnicalClass.SYMBOL, FORMAT, symbol.name() + " " + kept(symbol.id()));
    }

    static String footprintCode(LibrePcbLibrary.Package found, LibrePcbLibrary.Footprint footprint) {
        return ImportedNames.code(TechnicalClass.FOOTPRINT, FORMAT, found.name() + " " + kept(footprint.id()));
    }

    static String modelCode(LibrePcbLibrary.Package found, String modelId) {
        return ImportedNames.code(TechnicalClass.MODEL, FORMAT, found.name() + " " + kept(modelId));
    }

    static String component(String name, String id) {
        return ImportedNames.slug(FORMAT, name, kept(id));
    }

    static String design(String title) {
        return ImportedNames.slug(FORMAT, title);
    }

    static String pcb(String title) {
        return ImportedNames.slug(FORMAT, title, "pcb");
    }

    /** What a converted element writes as its source: {@code LibrePCB package R0805 06786894}. */
    static String source(String kind, String name, String id) {
        return SOURCE + kind + " " + name + " " + kept(id);
    }

    private static final String SOURCE = "LibrePCB ";
    /** The start of a footprint's UUID, as its slug keeps it before {@code -footprint}. */
    private static final Pattern FOOTPRINT_ID_IN_SLUG = Pattern.compile("-([0-9a-f]{" + ID_KEPT + "})-footprint$");

    /**
     * The code an element imported before imports named elements by codes is named by, from its source and its slug. A symbol
     * and a model as their codes are made — their name and the start of their UUID, both in the source (a model whose
     * package names it otherwise is named by its own name). A footprint by its package's name and the start of its own UUID,
     * which only its slug keeps — the source keeps the package's.
     */
    static Optional<String> codeOfImported(DocumentKind kind, String slug, String source) {
        if (source == null || !source.startsWith(SOURCE)) {
            return Optional.empty();
        }
        String[] said = source.substring(SOURCE.length()).split(" ", 2);
        if (said.length < 2 || said[1].lastIndexOf(' ') < 0) {
            return Optional.empty();
        }
        String name = said[1].substring(0, said[1].lastIndexOf(' '));
        String id = said[1].substring(said[1].lastIndexOf(' ') + 1);
        if (kind.equals(SchematicModule.SYMBOL) && said[0].equals("symbol")) {
            return Optional.of(ImportedNames.code(TechnicalClass.SYMBOL, FORMAT, name + " " + id));
        }
        if (kind.equals(ModelModule.MODEL) && said[0].equals("model")) {
            return Optional.of(ImportedNames.code(TechnicalClass.MODEL, FORMAT, name + " " + id));
        }
        Matcher footprintId = FOOTPRINT_ID_IN_SLUG.matcher(slug);
        if (kind.equals(PcbModule.FOOTPRINT) && said[0].equals("package") && footprintId.find()) {
            return Optional.of(ImportedNames.code(TechnicalClass.FOOTPRINT, FORMAT, name + " " + footprintId.group(1)));
        }
        return Optional.empty();
    }

    private static String kept(String id) {
        return id.substring(0, Math.min(ID_KEPT, id.length()));
    }
}
