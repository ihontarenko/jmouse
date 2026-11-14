package org.jmouse.cad.libre.pcb;

import org.jmouse.cad.document.DocumentKind;
import org.jmouse.cad.document.TechnicalClass;
import org.jmouse.cad.exchange.imported.ImportedNames;
import org.jmouse.cad.mesh.ModelModule;
import org.jmouse.cad.pcb.PcbModule;
import org.jmouse.cad.schematic.SchematicModule;
import org.jmouse.libre.pcb.LibrePcbLibrary;

import java.util.Arrays;
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

    /** What kind of element a converted element's source names, by the word the source writes. */
    enum SourceKind {
        SYMBOL("symbol"), COMPONENT("component"), PACKAGE("package"), MODEL("model");

        private final String word;

        SourceKind(String word) {
            this.word = word;
        }

        static Optional<SourceKind> of(String word) {
            return Arrays.stream(values()).filter(kind -> kind.word.equals(word)).findFirst();
        }
    }

    /** What a converted element writes as its source: {@code LibrePCB package R0805 06786894}. */
    static String source(SourceKind kind, String name, String id) {
        return SOURCE + kind.word + " " + name + " " + kept(id);
    }

    /**
     * The element a converted element's source names: what kind it is — {@code package}, {@code symbol}, {@code component},
     * {@code model} — and the start of its UUID; empty for a source that is no LibrePCB element's.
     */
    static Optional<SourcedElement> elementOf(String source) {
        if (source == null || !source.startsWith(SOURCE)) {
            return Optional.empty();
        }
        String written = source.substring(SOURCE.length()).strip();
        int kindEnd = written.indexOf(' ');
        int idAt = written.lastIndexOf(' ');
        if (kindEnd < 0 || idAt <= kindEnd) {
            return Optional.empty();
        }
        String name = written.substring(kindEnd + 1, idAt);
        String idStart = written.substring(idAt + 1);
        return SourceKind.of(written.substring(0, kindEnd)).map(kind -> new SourcedElement(kind, name, idStart));
    }

    /** An element a source names: its kind, its name, and the start of its UUID. */
    record SourcedElement(SourceKind kind, String name, String idStart) {
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
        Optional<SourcedElement> said = elementOf(source);
        if (said.isEmpty()) {
            return Optional.empty();
        }
        SourcedElement element = said.get();
        if (kind.equals(SchematicModule.SYMBOL) && element.kind() == SourceKind.SYMBOL) {
            return Optional.of(ImportedNames.code(TechnicalClass.SYMBOL, FORMAT, element.name() + " " + element.idStart()));
        }
        if (kind.equals(ModelModule.MODEL) && element.kind() == SourceKind.MODEL) {
            return Optional.of(ImportedNames.code(TechnicalClass.MODEL, FORMAT, element.name() + " " + element.idStart()));
        }
        Matcher footprintId = FOOTPRINT_ID_IN_SLUG.matcher(slug);
        if (kind.equals(PcbModule.FOOTPRINT) && element.kind() == SourceKind.PACKAGE && footprintId.find()) {
            return Optional.of(ImportedNames.code(TechnicalClass.FOOTPRINT, FORMAT, element.name() + " " + footprintId.group(1)));
        }
        return Optional.empty();
    }

    private static String kept(String id) {
        return id.substring(0, Math.min(ID_KEPT, id.length()));
    }
}
