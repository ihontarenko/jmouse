package org.jmouse.cad.kicad;

import org.jmouse.cad.component.Pin;
import org.jmouse.cad.component.PinBox;
import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.document.MetaLineNode;
import org.jmouse.cad.document.MetaNode;
import org.jmouse.cad.simulation.SimulationLines;
import org.jmouse.cad.simulation.SimulationModule;
import org.jmouse.cad.simulation.SimulationVocabulary;
import org.jmouse.el.grammar.ClauseBlockNode;
import org.jmouse.el.grammar.ClauseLineNode;
import org.jmouse.el.grammar.LineBuilder;
import org.jmouse.el.grammar.LineGrammar;
import org.jmouse.el.grammar.Nodes;
import org.jmouse.kicad.symbol.KiCadPin;
import org.jmouse.kicad.symbol.KiCadPinType;
import org.jmouse.kicad.symbol.KiCadSymbolDefinition;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * The component a KiCad symbol stands for when the library map names none: its package's pins, numbered as KiCad
 * numbers them, and the symbol it is drawn with — a part that can be placed, wired and stored, and that computes nothing
 * until its model is written. It is built as its document's tree.
 *
 * <p>A symbol the library map names a drawing for is drawn with that catalogue symbol, its pins named as the map names
 * them. Otherwise a chip — a symbol of several units, or one whose designator the map lists as boxed — is our box, its
 * pins placed by what they are ({@link PinBox}); only a design's own small symbol keeps the drawing it came with. A
 * symbol taken from KiCad's libraries rather than a design is always a box: its drawing is KiCad's, never ours.</p>
 */
public final class KiCadComponentStub {

    /** The names a chip's grounds and supplies go by, which a pin's kind alone does not always tell. */
    private static final String GROUND_NAMES = "GND.*|VSS.*|AGND|DGND|PGND";
    private static final String SUPPLY_NAMES = "VCC.*|VDD.*|VBAT|AVDD|DVDD|V\\+";

    /** Pins by their number as a package counts them: a number by its value, a grid name (A3) after, by its letters. */
    private static final Comparator<KiCadPin> PACKAGE_ORDER = Comparator
            .comparing((KiCadPin pin) -> !pin.number().matches("\\d+"))
            .thenComparingLong(pin -> pin.number().matches("\\d+") ? Long.parseLong(pin.number()) : 0)
            .thenComparing(KiCadPin::number);

    /** The side lines of a box, by their side. */
    private static final Map<PinBox.Side, String> SIDES = Map.of(PinBox.Side.LEFT, SimulationVocabulary.LEFT,
            PinBox.Side.RIGHT, SimulationVocabulary.RIGHT, PinBox.Side.TOP, SimulationVocabulary.TOP,
            PinBox.Side.BOTTOM, SimulationVocabulary.BOTTOM);

    /**
     * The catalogue symbol a library map's drawing resolved to — its code found in the catalogue — and the map's names
     * for the symbol's pins by KiCad's numbers.
     *
     * @param symbol the catalogue symbol's slug
     * @param pins   the symbol's pin by the KiCad pin's number
     */
    public record CatalogueDrawing(String symbol, Map<String, String> pins) {

        public CatalogueDrawing {
            pins = Map.copyOf(pins);
        }
    }

    private KiCadComponentStub() {
    }

    /**
     * The component's document, drawn with the catalogue symbol {@code drawing} is when there is one — see
     * {@link #drawnWith} —, its pins named as the drawing names them; its package landing on {@code footprint} when one
     * is given.
     */
    public static DocumentNode documentOf(KiCadSymbolDefinition symbol, Optional<String> footprint,
                                          Optional<CatalogueDrawing> drawing, boolean boxed, KiCadLibraries libraries) {
        Map<String, String> pinNames = drawing.map(CatalogueDrawing::pins).orElse(Map.of());
        Set<String> bubbled = new HashSet<>();
        List<Pin> pins = pinsOf(symbol, pinNames, bubbled);
        String summary = firstOf(symbol.property("Description"), symbol.property("ki_description"), symbol.name())
                + (drawing.isPresent() ? " — imported from KiCad: its pins, drawn with the catalogue's symbol, with no model yet"
                : " — imported from KiCad: its pins and its drawing, with no model yet");
        DocumentNode document = new DocumentNode(SimulationModule.COMPONENT, KiCadSymbolDrawing.titleOf(symbol),
                KiCadSymbolDrawing.componentSlugOf(symbol), true, null, 1);
        MetaNode meta = new MetaNode();
        meta.addExpression(MetaLineNode.of(MetaNode.SUMMARY, Nodes.text(summary)));
        meta.addExpression(MetaLineNode.of(MetaNode.CATEGORY, Nodes.word(libraries.category())));
        meta.addExpression(MetaLineNode.of(MetaNode.TAGS, Nodes.words(libraries.componentTags())));
        meta.addExpression(MetaLineNode.of(MetaNode.SOURCE, Nodes.text(KiCadSources.symbol(symbol.name()))));
        designatorOf(symbol).ifPresent(designator -> meta.addExpression(MetaLineNode.of(MetaNode.DESIGNATOR, Nodes.word(designator))));
        String written = symbol.property("Footprint");
        if (!written.isBlank()) {
            meta.addExpression(MetaLineNode.of("package", Nodes.text(written)));
        }
        document.addExpression(meta);
        ClauseBlockNode pinBlock = new ClauseBlockNode("pins", null);
        pinBlock.open();
        pins.forEach(pin -> pinBlock.addExpression(pinLine(pin)));
        document.addExpression(pinBlock);
        document.addExpression(drawing.map(found -> uses(found.symbol())).orElseGet(() -> symbolOf(symbol, pins, bubbled, boxed)));
        footprint.ifPresent(slug -> document.addExpression(new ClauseBlockNode("footprint",
                LineBuilder.line(SimulationLines.FOOTPRINT).slot("slug", Nodes.word(slug)).build())));
        return document;
    }

    /**
     * The drawing the library map names for a symbol, when it can draw it: the catalogue holds a symbol of the code the
     * map names, the map names every pin the symbol numbers but those KiCad marks as not connected, and each name is a
     * pin of the drawing. A drawing that would leave a connectable pin unplaced is no drawing — the part keeps KiCad's.
     */
    public static Optional<CatalogueDrawing> drawnWith(KiCadSymbolDefinition symbol, KiCadLibraryMap library,
                                                       KiCadCatalogue catalogue) {
        return library.drawingOf(symbol.name()).flatMap(drawing -> catalogue.symbolCoded(drawing.code())
                .flatMap(slug -> catalogue.symbolDrawing(slug)
                        .filter(drawn -> placesEveryPin(symbol, drawing, drawn))
                        .map(drawn -> new CatalogueDrawing(slug, drawing.pins()))));
    }

    /** Whether every pin the symbol can connect is named by the map and stands on the drawing. */
    private static boolean placesEveryPin(KiCadSymbolDefinition symbol, KiCadLibraryMap.Drawing drawing,
                                          KiCadCatalogue.PartDrawing drawn) {
        return symbol.pins().stream().filter(pin -> pin.type() != KiCadPinType.NOT_CONNECTED)
                .allMatch(pin -> drawing.pins().containsKey(pin.number())
                        && drawn.pins().containsKey(drawing.pins().get(pin.number())));
    }

    /**
     * Every pad once, named by its number as the part's connections name it — or as {@code pinNames} names that number —,
     * KiCad's own name its label; {@code bubbled} gains the pins a box draws with an inverting bubble.
     */
    private static List<Pin> pinsOf(KiCadSymbolDefinition symbol, Map<String, String> pinNames, Set<String> bubbled) {
        List<Pin> pins = new ArrayList<>();
        Set<String> named = new LinkedHashSet<>();
        for (int unit : KiCadSymbolDrawing.units(symbol)) {
            // KiCad keeps a unit's pins in the order they were drawn; the package's order is by number, 1, 2, … 10.
            List<KiCadPin> unitPins = symbol.pinsOf(unit).stream().sorted(PACKAGE_ORDER).toList();
            for (KiCadPin pin : unitPins) {
                String name = pinNames.getOrDefault(pin.number(), KiCadSymbolDrawing.pinName(pin));
                if (!named.add(name)) {
                    continue;
                }
                List<List<String>> numbers = pin.number().matches("\\d+") ? List.of(List.of(pin.number())) : List.of();
                String label = KiCadPin.NO_NAME.equals(pin.name()) ? "" : pin.name();
                String shape = pin.shape() == null ? "" : pin.shape();
                if (shape.startsWith("inverted")) {
                    bubbled.add(name);
                }
                pins.add(new Pin(name, null, natureOf(pin), directionOf(pin), null, numbers, shape.contains("clock"), label));
            }
        }
        return pins;
    }

    /** {@code name nature [direction] [number] [edge] ["label"]}: an input says no direction, as a pin read so is one. */
    private static ClauseLineNode pinLine(Pin pin) {
        LineBuilder line = LineBuilder.line(SimulationLines.PIN).slot("name", Nodes.name(pin.name()))
                .slot("nature", Nodes.word(pin.nature()));
        if (!pin.direction().equals(SimulationVocabulary.INPUT)) {
            line.slot("direction", Nodes.word(pin.direction()));
        }
        if (!pin.numbers().isEmpty()) {
            line.slot("numbers", Nodes.groups(pin.numbers()));
        }
        if (pin.edge()) {
            line.flag(SimulationVocabulary.EDGE);
        }
        if (!pin.label().isEmpty()) {
            line.ending(Nodes.text(pin.label()));
        }
        return line.build();
    }

    /** A design's own symbol of one unit: its drawing. A chip: our box, its pins placed by what they are. */
    private static ClauseBlockNode symbolOf(KiCadSymbolDefinition symbol, List<Pin> pins, Set<String> bubbled, boolean boxed) {
        List<Integer> units = KiCadSymbolDrawing.units(symbol);
        if (!boxed && units.size() == 1) {
            return uses(KiCadSymbolDrawing.slugOf(symbol, units.getFirst()));
        }
        ClauseBlockNode box = new ClauseBlockNode("symbol", LineBuilder.line(SimulationLines.SYMBOL).flag("box").build());
        box.open();
        PinBox.sides(pins).forEach((side, placed) -> {
            LineGrammar grammar = SimulationLines.BOX_SIDES.stream().filter(line -> line.word().equals(SIDES.get(side)))
                    .findFirst().orElseThrow();
            List<List<String>> items = placed.stream().map(pin -> {
                List<String> item = new ArrayList<>(List.of(pin.name()));
                if (pin.edge()) {
                    item.add(SimulationVocabulary.EDGE);
                }
                if (bubbled.contains(pin.name())) {
                    item.add(SimulationVocabulary.BUBBLE);
                }
                return (List<String>) item;
            }).toList();
            box.addExpression(LineBuilder.line(grammar).slot("pins", Nodes.groups(items)).build());
        });
        return box;
    }

    /** {@code symbol uses slug}. */
    private static ClauseBlockNode uses(String symbol) {
        return new ClauseBlockNode("symbol", LineBuilder.line(SimulationLines.SYMBOL).clause("uses", Nodes.words(List.of(symbol)))
                .build());
    }

    /** The numbers its pins carry, each once: what the pads of the footprint its package lands on must carry. */
    public static Set<String> pinNumbers(KiCadSymbolDefinition symbol) {
        Set<String> numbers = new LinkedHashSet<>();
        symbol.pins().stream().sorted(PACKAGE_ORDER).forEach(pin -> numbers.add(pin.number()));
        return numbers;
    }

    /**
     * Whether a part placed from this symbol is drawn as our box: a symbol of several units — a part is drawn with one
     * symbol —, or a chip, its designator one the library map lists as boxed ({@code boxed-designators U}).
     */
    public static boolean boxed(KiCadSymbolDefinition symbol, KiCadLibraryMap library) {
        return KiCadSymbolDrawing.units(symbol).size() > 1
                || designatorOf(symbol).filter(library.boxedDesignators()::contains).isPresent();
    }

    /**
     * A power pin is a supply or a ground by its name. A passive pin named as one is one too: KiCad marks a chip's extra
     * VSS pins passive, so they are not fought over by its power flags, and a box must still put them at the bottom.
     */
    private static String natureOf(KiCadPin pin) {
        String name = pin.name().toUpperCase(Locale.ROOT);
        return switch (pin.type()) {
            case POWER_INPUT, POWER_OUTPUT -> name.matches(GROUND_NAMES) ? SimulationVocabulary.GROUND : SimulationVocabulary.POWER;
            case PASSIVE -> name.matches(GROUND_NAMES) ? SimulationVocabulary.GROUND
                    : name.matches(SUPPLY_NAMES) ? SimulationVocabulary.POWER : SimulationVocabulary.ANALOG;
            default -> SimulationVocabulary.ANALOG;
        };
    }

    private static String directionOf(KiCadPin pin) {
        return switch (pin.type()) {
            case OUTPUT, POWER_OUTPUT, OPEN_COLLECTOR, OPEN_EMITTER -> SimulationVocabulary.OUTPUT;
            case BIDIRECTIONAL -> SimulationVocabulary.BIDIRECTIONAL;
            // A three-state pin drives its line or lets it go: an output that may float, never one that reads.
            case TRI_STATE -> SimulationVocabulary.OUTPUT;
            default -> SimulationVocabulary.INPUT;
        };
    }

    /** The letters a reference starts with: {@code U} of {@code U3}, {@code Q} of {@code Q?}. */
    private static Optional<String> designatorOf(KiCadSymbolDefinition symbol) {
        String letters = symbol.property("Reference").replaceAll("[^A-Za-z].*$", "");
        return letters.isEmpty() ? Optional.empty() : Optional.of(letters);
    }

    private static String firstOf(String... candidates) {
        for (String candidate : candidates) {
            if (candidate != null && !candidate.isBlank()) {
                return candidate.strip();
            }
        }
        return "";
    }
}
