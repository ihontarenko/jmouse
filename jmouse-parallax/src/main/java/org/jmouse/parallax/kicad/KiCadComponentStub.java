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
 * them. Otherwise a symbol of one unit is drawn by the symbol imported from KiCad, and a symbol of several is a box: each
 * unit has its own drawing, and a part is drawn with one symbol.</p>
 */
public final class KiCadComponentStub {

    /** Where a pin of each kind stands on a box: what comes in on the left, what goes out on the right. */
    private static final String LEFT   = "left";
    private static final String RIGHT  = "right";
    private static final String TOP    = "top";
    private static final String BOTTOM = "bottom";

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
        return documentOf(symbol, footprint, Optional.empty());
    }

    /**
     * The component's document, drawn with the catalogue symbol {@code drawing} names when there is one — see
     * {@link #drawnWith} —, its pins named as the drawing names them.
     */
    public static ExperienceDocument documentOf(KiCadSymbolDefinition symbol, Optional<String> footprint,
                                                Optional<KiCadLibraryMap.Drawing> drawing) {
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
                .orElseGet(() -> symbolOf(symbol, pins));
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
                pins.add(new Pin(name, numbers, natureOf(pin), directionOf(pin), List.of(), description));
            }
        }
        return pins;
    }

    /** One unit: KiCad's drawing of it. Several: a box, since a part is drawn with one symbol and each unit has its own. */
    private static Block.Symbol symbolOf(KiCadSymbolDefinition symbol, List<Pin> pins) {
        List<Integer> units = KiCadSymbolDrawing.units(symbol);
        if (units.size() == 1) {
            return new Block.Symbol(List.of(KiCadSymbolDrawing.slugOf(symbol, units.getFirst())), Map.of(), Optional.empty(),
                    Map.of(), 0, 0, List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), Map.of());
        }
        Map<String, List<String>> sides = new LinkedHashMap<>();
        for (Pin pin : pins) {
            sides.computeIfAbsent(sideOf(pin), ignored -> new ArrayList<>()).add(pin.name());
        }
        return new Block.Symbol(List.of(), Map.of(), Optional.of("box"), sides, 0, 0, List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(), List.of(), Map.of());
    }

    /** The numbers its pins carry, each once: what the pads of the footprint its package lands on must carry. */
    public static Set<String> pinNumbers(KiCadSymbolDefinition symbol) {
        Set<String> numbers = new LinkedHashSet<>();
        symbol.pins().stream().sorted(PACKAGE_ORDER).forEach(pin -> numbers.add(pin.number()));
        return numbers;
    }

    /** Whether a part placed from this symbol is drawn by KiCad's symbol: only a symbol of one unit is. */
    public static boolean drawnByKiCad(KiCadSymbolDefinition symbol) {
        return KiCadSymbolDrawing.units(symbol).size() == 1;
    }

    private static String sideOf(Pin pin) {
        if (pin.nature() == PinNature.POWER) {
            return pin.direction() == PinDirection.OUTPUT ? RIGHT : TOP;
        }
        if (pin.nature() == PinNature.GROUND) {
            return BOTTOM;
        }
        return pin.direction() == PinDirection.OUTPUT ? RIGHT : LEFT;
    }

    private static PinNature natureOf(KiCadPin pin) {
        return switch (pin.type()) {
            case POWER_INPUT, POWER_OUTPUT -> pin.name().toUpperCase(Locale.ROOT).matches("GND.*|VSS.*|AGND|DGND|PGND")
                    ? PinNature.GROUND : PinNature.POWER;
            default -> PinNature.ANALOG;
        };
    }

    private static PinDirection directionOf(KiCadPin pin) {
        return switch (pin.type()) {
            case OUTPUT, POWER_OUTPUT, OPEN_COLLECTOR, OPEN_EMITTER -> PinDirection.OUTPUT;
            case BIDIRECTIONAL, TRI_STATE -> PinDirection.BIDIRECTIONAL;
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
