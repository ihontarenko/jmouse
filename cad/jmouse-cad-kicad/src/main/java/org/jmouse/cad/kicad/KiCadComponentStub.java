package org.jmouse.cad.kicad;

import org.jmouse.cad.exchange.imported.ImportedComponent;
import org.jmouse.cad.exchange.imported.ImportedNames;
import org.jmouse.cad.exchange.imported.ImportedPins;
import org.jmouse.cad.component.Pin;
import org.jmouse.cad.component.PinBox;
import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.simulation.SimulationVocabulary;
import org.jmouse.kicad.symbol.KiCadPin;
import org.jmouse.kicad.symbol.KiCadPinType;
import org.jmouse.kicad.symbol.KiCadSymbolDefinition;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
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
 * pins placed by what they are ({@link PinBox}); a symbol of one unit keeps the drawing it came with — a converted
 * drawing, which says whose it is ({@link KiCadLibraries#provenance}).</p>
 */
public final class KiCadComponentStub {

    /** Pins by their number as a package counts them: a number by its value, a grid name (A3) after, by its letters. */
    private static final Comparator<KiCadPin> PACKAGE_ORDER = Comparator
            .comparing((KiCadPin pin) -> !pin.number().matches("\\d+"))
            .thenComparingLong(pin -> pin.number().matches("\\d+") ? Long.parseLong(pin.number()) : 0)
            .thenComparing(KiCadPin::number);

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
     * is given. {@code installed} says the symbol is read from KiCad's installed libraries, and so is theirs.
     */
    public static DocumentNode documentOf(KiCadSymbolDefinition symbol, Optional<String> footprint,
                                          Optional<CatalogueDrawing> drawing, boolean boxed, KiCadLibraries libraries,
                                          boolean installed) {
        Map<String, String> pinNames = drawing.map(CatalogueDrawing::pins).orElse(Map.of());
        Set<String> bubbled = new HashSet<>();
        List<Pin> pins = pinsOf(symbol, pinNames, bubbled);
        String summary = firstOf(symbol.property("Description"), symbol.property("ki_description"), symbol.name())
                + (drawing.isPresent() ? " — imported from KiCad: its pins, drawn with the catalogue's symbol, with no model yet"
                : " — imported from KiCad: its pins and its drawing, with no model yet");
        List<Integer> units = KiCadSymbolDrawing.units(symbol);
        // A design's own symbol of one unit keeps its drawing; a chip is our box, its pins placed by what they are.
        ImportedComponent.Drawing drawn = drawing.<ImportedComponent.Drawing>map(found -> new ImportedComponent.Drawing.Uses(found.symbol()))
                .orElseGet(() -> !boxed && units.size() == 1
                        ? new ImportedComponent.Drawing.Uses(KiCadSymbolDrawing.codeOf(symbol, units.getFirst()))
                        : new ImportedComponent.Drawing.Box());
        return new ImportedComponent(KiCadSymbolDrawing.titleOf(symbol), KiCadSymbolDrawing.componentSlugOf(symbol), summary,
                libraries.provenance(KiCadSources.symbol(symbol.name()), installed), designatorOf(symbol).orElse(null),
                symbol.property("Footprint"), pins, bubbled, drawn, footprint.orElse(null)).document(libraries);
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
                        .map(drawn -> new CatalogueDrawing(drawing.code(), drawing.pins()))));
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

    /** A pin's nature, as KiCad's kind of it and its name say ({@link ImportedPins}). */
    private static String natureOf(KiCadPin pin) {
        ImportedPins.Role role = switch (pin.type()) {
            case POWER_INPUT, POWER_OUTPUT -> ImportedPins.Role.POWER;
            case PASSIVE -> ImportedPins.Role.PASSIVE;
            default -> ImportedPins.Role.SIGNAL;
        };
        return ImportedPins.natureOf(pin.name(), role);
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
        return ImportedNames.designatorOf(symbol.property("Reference"));
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
