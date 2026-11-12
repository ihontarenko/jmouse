package org.jmouse.kicad.library;

import org.jmouse.kicad.pcb.KiCadPcb;
import org.jmouse.kicad.footprint.KiCadFootprintField;
import org.jmouse.kicad.KiCadPoint;
import org.jmouse.kicad.KiCadProperty;
import org.jmouse.kicad.footprint.KiCadFootprint;
import org.jmouse.kicad.footprint.KiCadFootprintGraphic;
import org.jmouse.kicad.footprint.KiCadModel;
import org.jmouse.kicad.footprint.KiCadPad;
import org.jmouse.sexpression.SExpressionDocument;
import org.jmouse.sexpression.SExpressionPrinter;
import org.jmouse.sexpression.node.SExpressionAtom;
import org.jmouse.sexpression.node.SExpressionList;
import org.jmouse.sexpression.node.SExpressionNode;
import org.jmouse.sexpression.node.SExpressionString;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.UUID;

/**
 * 📚 The libraries a KiCad project carries of its own, beside its files: the footprints and symbols its design draws that no
 * KiCad library holds, written as KiCad keeps a library — a {@code .pretty} folder of one {@code .kicad_mod} a footprint, a
 * {@code .kicad_sym} of its symbols — and named in the project's {@code fp-lib-table} and {@code sym-lib-table} by a nickname
 * found under the project's own folder ({@code ${KIPRJMOD}}). So the project opens whole on any KiCad, its parts editable
 * there, and its symbols and footprints update from their library as KiCad's own do.
 */
public final class KiCadProjectLibraries {

    /** KiCad 8's footprint file: the version a {@code .kicad_mod} is written as, the same as its board. */
    private static final long FOOTPRINT_VERSION = 20240108L;
    /** KiCad 8's symbol library. */
    private static final long SYMBOL_LIBRARY_VERSION = 20231120L;
    /** KiCad's library tables, as KiCad 8 writes them. */
    private static final int TABLE_VERSION = 7;
    private static final String GENERATOR = "jmouse";

    /** Where a project's own library is found: the folder of the project file, as KiCad says it. */
    private static final String PROJECT_FOLDER = "${KIPRJMOD}";
    private static final String FOOTPRINT_FOLDER = ".pretty";
    private static final String FOOTPRINT_FILE = ".kicad_mod";
    private static final String SYMBOL_FILE = ".kicad_sym";
    public static final String FOOTPRINT_TABLE = "fp-lib-table";
    public static final String SYMBOL_TABLE = "sym-lib-table";

    /** Where a footprint's two fields print in a library: its reference above it, its value below. */
    private static final double FIELD_OFFSET = 3;
    private static final double FIELD_HEIGHT = 1;
    private static final double FIELD_THICKNESS = 0.15;

    private KiCadProjectLibraries() {
    }

    /** The folder a library of footprints is, beside the project: {@code jmCAD.pretty}. */
    public static String footprintFolder(String nickname) {
        return nickname + FOOTPRINT_FOLDER;
    }

    /** The file a footprint is in that folder: {@code jmCAD.pretty/trimmer-bourns-3296w-footprint.kicad_mod}. */
    public static String footprintFile(String nickname, String name) {
        return footprintFolder(nickname) + "/" + name + FOOTPRINT_FILE;
    }

    /** The file a library of symbols is, beside the project: {@code jmCAD.kicad_sym}. */
    public static String symbolFile(String nickname) {
        return nickname + SYMBOL_FILE;
    }

    /**
     * A footprint as its library keeps it — standing on the front, unturned —: its reference and value fields, every other
     * field it carries hidden (whose it is, its licence, its source), how it is mounted, its drawing, its pads on no net, its
     * models.
     *
     * @param referenceLayer the layer its reference prints on: the front's silkscreen
     * @param valueLayer     the layer its value prints on: the front's fabrication drawing
     */
    public static String footprint(KiCadFootprint footprint, String referenceLayer, String valueLayer) {
        String seed = "footprint " + footprint.name();
        List<SExpressionNode> items = new ArrayList<>();
        items.add(SExpressionString.of(footprint.name()));
        items.add(SExpressionList.of("version", SExpressionAtom.of(FOOTPRINT_VERSION)));
        items.add(SExpressionList.of("generator", SExpressionString.of(GENERATOR)));
        items.add(SExpressionList.of("layer", SExpressionString.of("F.Cu")));
        if (!footprint.description().isBlank()) {
            items.add(SExpressionList.of("descr", SExpressionString.of(footprint.description())));
        }
        if (!footprint.tags().isEmpty()) {
            items.add(SExpressionList.of("tags", SExpressionString.of(String.join(" ", footprint.tags()))));
        }
        // The reference and the value where the footprint prints them; one that says nothing, over and under its origin.
        items.add(field(REFERENCE, "REF**", placeOf(footprint, KiCadProperty.REFERENCE, -FIELD_OFFSET, referenceLayer), false, seed));
        items.add(field(VALUE, footprint.name(), placeOf(footprint, KiCadProperty.VALUE, FIELD_OFFSET, valueLayer), false, seed));
        KiCadFootprintField kept = new KiCadFootprintField(KiCadProperty.VALUE, new KiCadPoint(0, 0), 0, valueLayer, FIELD_HEIGHT, true);
        footprint.properties().forEach((name, text) -> {
            if (!REFERENCE.equals(name) && !VALUE.equals(name)) {
                items.add(field(name, text, kept, true, seed));
            }
        });
        footprint.mounting().ifPresent(mounting -> items.add(SExpressionList.of("attr", SExpressionAtom.of(mounting))));
        for (KiCadFootprintGraphic graphic : footprint.graphics()) {
            items.add(graphic.written(false, identity(seed + " graphic " + items.size())));
        }
        for (KiCadPad pad : footprint.pads()) {
            items.add(pad.written(0, KiCadPad.NO_NET, identity(seed + " pad " + pad.number())));
        }
        footprint.models().stream().map(KiCadModel::written).forEach(items::add);
        return print(footprint.name() + FOOTPRINT_FILE, SExpressionList.of("footprint", items.toArray(SExpressionNode[]::new)));
    }

    /**
     * A library of symbols, each as a schematic keeps its copy ({@code lib_symbols}) — {@code jmCAD:1n4007} — named in the
     * library by what follows its nickname: {@code 1n4007}.
     *
     * @param symbols each symbol's copy, by its full name
     */
    public static String symbols(String nickname, Map<String, SExpressionList> symbols) {
        List<SExpressionNode> items = new ArrayList<>();
        items.add(SExpressionList.of("version", SExpressionAtom.of(SYMBOL_LIBRARY_VERSION)));
        items.add(SExpressionList.of("generator", SExpressionString.of(GENERATOR)));
        symbols.forEach((name, symbol) -> items.add(renamed(symbol, name.startsWith(nickname + ":") ? name.substring(nickname.length() + 1) : name)));
        return print(symbolFile(nickname), SExpressionList.of("kicad_symbol_lib", items.toArray(SExpressionNode[]::new)));
    }

    /** The project's table of footprint libraries, naming its own: {@code (lib (name "jmCAD") (uri "${KIPRJMOD}/jmCAD.pretty") …)}. */
    public static String footprintTable(String nickname, String description) {
        return table("fp_lib_table", nickname, PROJECT_FOLDER + "/" + footprintFolder(nickname), description);
    }

    /** The project's table of symbol libraries, naming its own. */
    public static String symbolTable(String nickname, String description) {
        return table("sym_lib_table", nickname, PROJECT_FOLDER + "/" + symbolFile(nickname), description);
    }

    private static String table(String kind, String nickname, String uri, String description) {
        SExpressionList library = SExpressionList.of("lib", SExpressionList.of("name", SExpressionString.of(nickname)),
                SExpressionList.of("type", SExpressionString.of("KiCad")), SExpressionList.of("uri", SExpressionString.of(uri)),
                SExpressionList.of("options", SExpressionString.of("")), SExpressionList.of("descr", SExpressionString.of(description)));
        return print(kind, SExpressionList.of(kind, SExpressionList.of("version", SExpressionAtom.of(TABLE_VERSION)), library));
    }

    /** A copy of a symbol under another name: its first word after {@code symbol}. */
    private static SExpressionList renamed(SExpressionList symbol, String name) {
        List<SExpressionNode> items = new ArrayList<>(symbol.items());
        items.set(1, SExpressionString.of(name));
        return new SExpressionList(items, symbol.closingLeading(), symbol.position(), symbol.leading());
    }

    /** Where a footprint prints one of its fields: its own place, else on its axis {@code offset} from its origin, on {@code layer}. */
    private static KiCadFootprintField placeOf(KiCadFootprint footprint, KiCadProperty property, double offset, String layer) {
        return footprint.fields().stream().filter(field -> field.property() == property).findFirst()
                .orElseGet(() -> new KiCadFootprintField(property, new KiCadPoint(0, offset), 0, layer, FIELD_HEIGHT, false));
    }

    private static SExpressionList field(String name, String text, KiCadFootprintField place, boolean hidden, String seed) {
        List<SExpressionNode> items = new ArrayList<>();
        items.add(SExpressionString.of(name));
        items.add(SExpressionString.of(text));
        items.add(SExpressionList.of("at", SExpressionAtom.of(place.at().x()), SExpressionAtom.of(place.at().y()),
                SExpressionAtom.of(place.rotation())));
        items.add(SExpressionList.of("layer", SExpressionString.of(place.layer())));
        if (hidden || place.hidden()) {
            items.add(SExpressionList.of("hide", SExpressionAtom.of("yes")));
        }
        items.add(SExpressionList.of("uuid", SExpressionString.of(identity(seed + " " + name))));
        List<SExpressionNode> effects = new ArrayList<>();
        effects.add(SExpressionList.of("font",
                SExpressionList.of("size", SExpressionAtom.of(place.height()), SExpressionAtom.of(place.height())),
                // KiCad's stroke keeps its share of the letters' height.
                SExpressionList.of("thickness", SExpressionAtom.of(place.height() * FIELD_THICKNESS / FIELD_HEIGHT))));
        justified(place).ifPresent(effects::add);
        items.add(SExpressionList.of("effects", effects.toArray(SExpressionNode[]::new)));
        return SExpressionList.of("property", items.toArray(SExpressionNode[]::new));
    }

    /** How a field stands on its point, as KiCad writes it; nothing for one on its middle, as KiCad places one by default. */
    private static Optional<SExpressionList> justified(KiCadFootprintField place) {
        List<SExpressionNode> words = KiCadPcb.justifyWords(place.along(), place.across());
        return words.isEmpty() ? Optional.empty() : Optional.of(SExpressionList.of("justify", words.toArray(SExpressionNode[]::new)));
    }

    /** An identity made from what it is the identity of: the same library written twice is the same file. */
    private static String identity(String seed) {
        return UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8)).toString();
    }

    private static String print(String name, SExpressionList root) {
        return new SExpressionPrinter().print(SExpressionDocument.of(name, root));
    }

    /** The two fields KiCad gives every footprint. */
    private static final String REFERENCE = "Reference";
    private static final String VALUE = "Value";
}
