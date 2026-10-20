package org.jmouse.kicad.schematic;

import static org.jmouse.kicad.KiCadKeyword.*;

import org.jmouse.kicad.KiCadPoint;
import org.jmouse.kicad.parser.KiCadDocument;
import org.jmouse.kicad.parser.node.KiCadList;
import org.jmouse.kicad.symbol.KiCadSymbolDefinition;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * One schematic file — a {@code .kicad_sch} —, read as a model: the symbols placed on it with the library copies they
 * are drawn from, its wires, junctions and no-connect marks, its labels, and the sheets it places. It is read from the
 * document's tree and keeps the tree, so nothing the model does not name is lost to a consumer that needs it.
 *
 * <p>KiCad 7 (format 20230121) and newer; a file of an older format is refused, naming its version.</p>
 */
public final class KiCadSchematic {

    /** KiCad 7's format: the first this model reads. */
    private static final long FIRST_VERSION = 20230121L;

    private final KiCadDocument document;
    private final long version;
    private final String uuid;
    private final Map<String, KiCadSymbolDefinition> librarySymbols;
    private final List<KiCadSymbolInstance> symbols;
    private final List<KiCadWire> wires;
    private final List<KiCadPoint> junctions;
    private final List<KiCadPoint> noConnects;
    private final List<KiCadLabel> labels;
    private final List<KiCadSheet> sheets;

    private KiCadSchematic(KiCadDocument document, long version, String uuid, Map<String, KiCadSymbolDefinition> librarySymbols,
                           List<KiCadSymbolInstance> symbols, List<KiCadWire> wires, List<KiCadPoint> junctions,
                           List<KiCadPoint> noConnects, List<KiCadLabel> labels, List<KiCadSheet> sheets) {
        this.document = document;
        this.version = version;
        this.uuid = uuid;
        this.librarySymbols = librarySymbols;
        this.symbols = symbols;
        this.wires = wires;
        this.junctions = junctions;
        this.noConnects = noConnects;
        this.labels = labels;
        this.sheets = sheets;
    }

    /**
     * The schematic a document holds.
     *
     * @throws IllegalArgumentException when it is not a schematic, or one older than KiCad 7
     */
    public static KiCadSchematic of(KiCadDocument document) {
        KiCadList root = document.root();
        if (!root.is(SCHEMATIC_FILE)) {
            throw new IllegalArgumentException(document.name() + " is a " + document.kind() + ", not a schematic.");
        }
        long version = (long) root.requireList(VERSION).number(1);
        if (version < FIRST_VERSION) {
            throw new IllegalArgumentException(document.name() + " is of format " + version + ": KiCad 7 (" + FIRST_VERSION
                    + ") and newer are read; open it in a newer KiCad and save it.");
        }
        Map<String, KiCadSymbolDefinition> librarySymbols = new LinkedHashMap<>();
        root.list(LIBRARY_SYMBOLS).ifPresent(library -> library.lists(SYMBOL).forEach(symbol -> {
            KiCadSymbolDefinition definition = KiCadSymbolDefinition.of(symbol);
            librarySymbols.put(definition.name(), definition);
        }));
        List<KiCadLabel> labels = new ArrayList<>();
        for (KiCadLabel.Kind kind : KiCadLabel.Kind.values()) {
            root.lists(kind.keyword()).forEach(label -> labels.add(KiCadLabel.of(kind, label)));
        }
        return new KiCadSchematic(document, version, root.wordOf(UUID).orElse(""), librarySymbols,
                root.lists(SYMBOL).stream().map(KiCadSymbolInstance::of).toList(),
                root.lists(WIRE).stream().map(KiCadWire::of).toList(),
                root.lists(JUNCTION).stream().map(junction -> KiCadPoint.of(junction.requireList(AT))).toList(),
                root.lists(NO_CONNECT).stream().map(mark -> KiCadPoint.of(mark.requireList(AT))).toList(),
                List.copyOf(labels), root.lists(SHEET).stream().map(KiCadSheet::of).toList());
    }

    public KiCadDocument document() {
        return document;
    }

    public String name() {
        return document.name();
    }

    public long version() {
        return version;
    }

    public String uuid() {
        return uuid;
    }

    public List<KiCadSymbolInstance> symbols() {
        return symbols;
    }

    public List<KiCadWire> wires() {
        return wires;
    }

    public List<KiCadPoint> junctions() {
        return junctions;
    }

    public List<KiCadPoint> noConnects() {
        return noConnects;
    }

    public List<KiCadLabel> labels() {
        return labels;
    }

    public List<KiCadSheet> sheets() {
        return sheets;
    }

    /** The library copy a placed symbol is drawn from. */
    public Optional<KiCadSymbolDefinition> definitionOf(KiCadSymbolInstance symbol) {
        return Optional.ofNullable(librarySymbols.get(symbol.libraryId()));
    }

    /** Every library symbol this schematic keeps a copy of, by its library id. */
    public Map<String, KiCadSymbolDefinition> librarySymbols() {
        return Map.copyOf(librarySymbols);
    }
}
