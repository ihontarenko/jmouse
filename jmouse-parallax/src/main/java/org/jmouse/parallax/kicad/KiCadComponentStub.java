package org.jmouse.parallax.kicad;

import org.jmouse.kicad.symbol.KiCadPin;
import org.jmouse.kicad.symbol.KiCadPinType;
import org.jmouse.kicad.symbol.KiCadSymbolDefinition;
import org.jmouse.parallax.script.Block;
import org.jmouse.parallax.script.Block.Pin;
import org.jmouse.parallax.script.Block.PinDirection;
import org.jmouse.parallax.script.Block.PinNature;
import org.jmouse.parallax.script.ExperienceDocument;
import org.jmouse.parallax.script.Kind;
import org.jmouse.parallax.script.Meta;
import org.jmouse.parallax.script.Meta.MetaValue;
import org.jmouse.parallax.symbol.PinBox;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * The component a KiCad symbol stands for when the library map names none: its package's pins, numbered as KiCad
 * numbers them, and the symbol KiCad draws it with — a part that can be placed, wired and stored, and that computes
 * nothing until its model is written. It is built as a jMX document and written by jMX's own writer.
 *
 * <p>A symbol the library map names a drawing for is drawn with that catalogue symbol, its pins named as the map names
 * them. Otherwise a chip — a symbol of several units, or one whose designator the map lists as boxed — is our box,
 * its pins placed by what they are ({@link PinBox}); only a design's own small symbol keeps the drawing it came with.
 * A symbol taken from KiCad's libraries rather than a design is always a box: its drawing is KiCad's, never ours.</p>
 */
public final class KiCadComponentStub {

    /** The words a box draws a clock's edge and an inverting bubble with. */
    private static final String EDGE = "edge";
    private static final String BUBBLE = "bubble";

    /** The names a chip's grounds and supplies go by, which a pin's kind alone does not always tell. */
    private static final String GROUND_NAMES = "GND.*|VSS.*|AGND|DGND|PGND";
    private static final String SUPPLY_NAMES = "VCC.*|VDD.*|VBAT|AVDD|DVDD|V\\+";

    /** Pins by their number as a package counts them: a number by its value, a grid name (A3) after, by its letters. */
    private static final Comparator<KiCadPin> PACKAGE_ORDER = Comparator
            .comparing((KiCadPin pin) -> !pin.number().matches("\\d+"))
            .thenComparingLong(pin -> pin.number().matches("\\d+") ? Long.parseLong(pin.number()) : 0)
            .thenComparing(KiCadPin::number);

    private KiCadComponentStub() {
    }

    /** The component's document: what it is called, what it says of itself, its pins and how it is drawn. */
    public static ExperienceDocument documentOf(KiCadSymbolDefinition symbol) {
        return documentOf(symbol, Optional.empty());
    }

    /**
     * The component's document, its package landing on a footprint: {@code footprint kicad-…-footprint}, the footprint
     * imported from the one KiCad's parts of it name.
     */
    public static ExperienceDocument documentOf(KiCadSymbolDefinition symbol, Optional<String> footprint) {
        return documentOf(symbol, footprint, Optional.empty(), true);
    }

    /**
     * The component's document, drawn with the catalogue symbol {@code drawing} names when there is one — see
     * {@link #drawnWith} —, its pins named as the drawing names them.
     */
    public static ExperienceDocument documentOf(KiCadSymbolDefinition symbol, Optional<String> footprint,
                                                Optional<KiCadLibraryMap.Drawing> drawing, boolean boxed) {
        Map<String, String> pinNames = drawing.map(KiCadLibraryMap.Drawing::pins).orElse(Map.of());
        List<Pin> pins = pinsOf(symbol, pinNames);
        String summary = firstOf(symbol.property("Description"), symbol.property("ki_description"), symbol.name())
                + (drawing.isPresent() ? " — imported from KiCad: its pins, drawn with the catalogue's symbol, with no model yet"
                : " — imported from KiCad: its pins and its drawing, with no model yet");
        Map<String, MetaValue> fields = new LinkedHashMap<>();
        designatorOf(symbol).ifPresent(designator -> fields.put("designator", new MetaValue.Names(List.of(designator))));
        String written = symbol.property("Footprint");
        if (!written.isBlank()) {
            fields.put("package", new MetaValue.Text(written));
        }
        Meta meta = new Meta(summary, null, "circuits", List.of("electronics", "kicad"), "KiCad symbol " + symbol.name(),
                List.of(), null, List.of(), fields, Map.of());
        Block.Symbol drawn = drawing.map(found -> new Block.Symbol(List.of(found.symbol()), Map.of(), Optional.empty(),
                Map.of(), 0, 0, List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), Map.of()))
                .orElseGet(() -> symbolOf(symbol, pins, boxed));
        List<Block> blocks = new ArrayList<>(List.of(new Block.Pins(pins), drawn));
        footprint.ifPresent(slug -> blocks.add(new Block.FootprintUse(slug)));
        return new ExperienceDocument(Kind.COMPONENT, KiCadSymbolDrawing.titleOf(symbol),
                KiCadSymbolDrawing.componentSlugOf(symbol),
                Optional.empty(), meta, Optional.empty(), List.of(), List.of(), blocks, 0);
    }

    /**
     * The drawing the library map names for a symbol, when it can draw it: the catalogue holds the drawing, the map names
     * every pin the symbol numbers but those KiCad marks as not connected, and each name is a pin of the drawing. A
     * drawing that would leave a connectable pin unplaced is no drawing — the part keeps KiCad's.
     */
    public static Optional<KiCadLibraryMap.Drawing> drawnWith(KiCadSymbolDefinition symbol, KiCadLibraryMap library,
                                                              KiCadCatalogue catalogue) {
        return library.drawingOf(symbol.name()).filter(drawing -> catalogue.symbolDrawing(drawing.symbol())
                .map(drawn -> symbol.pins().stream().filter(pin -> pin.type() != KiCadPinType.NOT_CONNECTED)
                        .allMatch(pin -> drawing.pins().containsKey(pin.number())
                                && drawn.pins().containsKey(drawing.pins().get(pin.number()))))
                .orElse(false));
    }

    /**
     * Every pad once, named by its number as the part's connections name it — or as {@code pinNames} names that number —,
     * KiCad's own name its description.
     */
    private static List<Pin> pinsOf(KiCadSymbolDefinition symbol, Map<String, String> pinNames) {
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
                List<Integer> numbers = pin.number().matches("\\d+") ? List.of(Integer.parseInt(pin.number())) : List.of();
                String description = KiCadPin.NO_NAME.equals(pin.name()) ? "" : pin.name();
                pins.add(new Pin(name, numbers, natureOf(pin), directionOf(pin), attributesOf(pin), description));
            }
        }
        return pins;
    }

    /** A design's own symbol of one unit: its drawing. A chip: our box, its pins placed by what they are. */
    private static Block.Symbol symbolOf(KiCadSymbolDefinition symbol, List<Pin> pins, boolean boxed) {
        List<Integer> units = KiCadSymbolDrawing.units(symbol);
        if (!boxed && units.size() == 1) {
            return new Block.Symbol(List.of(KiCadSymbolDrawing.slugOf(symbol, units.getFirst())), Map.of(), Optional.empty(),
                    Map.of(), 0, 0, List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), Map.of());
        }
        return new Block.Symbol(List.of(), Map.of(), Optional.of("box"), PinBox.sides(pins), 0, 0, List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(), List.of(), Map.of());
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

    /** What a pin's shape says the box must draw: a clock's edge, an inverting bubble. */
    private static List<String> attributesOf(KiCadPin pin) {
        String shape = pin.shape() == null ? "" : pin.shape();
        List<String> attributes = new ArrayList<>();
        if (shape.contains("clock")) {
            attributes.add(EDGE);
        }
        if (shape.startsWith("inverted")) {
            attributes.add(BUBBLE);
        }
        return attributes;
    }

    /**
     * A power pin is a supply or a ground by its name. A passive pin named as one is one too: KiCad marks a chip's extra
     * VSS pins passive, so they are not fought over by its power flags, and a box must still put them at the bottom.
     */
    private static PinNature natureOf(KiCadPin pin) {
        String name = pin.name().toUpperCase(Locale.ROOT);
        return switch (pin.type()) {
            case POWER_INPUT, POWER_OUTPUT -> name.matches(GROUND_NAMES) ? PinNature.GROUND : PinNature.POWER;
            case PASSIVE -> name.matches(GROUND_NAMES) ? PinNature.GROUND
                    : name.matches(SUPPLY_NAMES) ? PinNature.POWER : PinNature.ANALOG;
            default -> PinNature.ANALOG;
        };
    }

    private static PinDirection directionOf(KiCadPin pin) {
        return switch (pin.type()) {
            case OUTPUT, POWER_OUTPUT, OPEN_COLLECTOR, OPEN_EMITTER -> PinDirection.OUTPUT;
            case BIDIRECTIONAL -> PinDirection.BIDIRECTIONAL;
            // A three-state pin drives its line or lets it go: an output that may float, never one that reads.
            case TRI_STATE -> PinDirection.OUTPUT;
            default -> PinDirection.INPUT;
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
