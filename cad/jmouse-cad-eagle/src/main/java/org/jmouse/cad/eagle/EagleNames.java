package org.jmouse.cad.eagle;

import org.jmouse.cad.document.TechnicalClass;
import org.jmouse.cad.exchange.imported.ImportedNames;
import org.jmouse.cad.exchange.imported.ImportedPins;
import org.jmouse.cad.exchange.imported.UniqueIdentifiers;
import org.jmouse.eagle.EagleLibrary;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * What a document converted from Eagle is called — every slug opens with {@code eagle-}, its library's name in it, as
 * KiCad's open with {@code kicad-} —, what it writes as its source, and what an Eagle name becomes where jM needs one: a
 * pin's name, a pad's number.
 */
final class EagleNames {

    /** What every slug of a document converted from Eagle opens with. */
    private static final String EAGLE = "eagle";

    /** What a pin's name opens with where Eagle's opens with no letter: {@code 1} is {@code pin1}. */
    private static final String PIN = "pin";

    private EagleNames() {
    }

    /** A design's circuit: {@code Qwiic Button} is {@code eagle-qwiic-button}. */
    static String design(String title) {
        return ImportedNames.slug(EAGLE, title);
    }

    /** The pcb laid out from a design's circuit: {@code Qwiic Button} is {@code eagle-qwiic-button-pcb}. */
    static String pcb(String title) {
        return ImportedNames.slug(EAGLE, title, "pcb");
    }

    /** {@code SparkFun-Resistors}, {@code RESISTOR}, {@code 0603} is {@code eagle-sparkfun-resistors-resistor-0603}. */
    static String component(String library, String deviceSet, String device) {
        // A device's name goes on from its set's: 2.2KOHM and -0603-1/10W-1% are 2.2KOHM-0603-1/10W-1%, one name.
        return ImportedNames.slug(EAGLE, library, deviceSet + device);
    }

    static String symbol(String library, String symbol) {
        return ImportedNames.slug(EAGLE, library, symbol, "symbol");
    }

    static String footprint(String library, String packageName) {
        return ImportedNames.slug(EAGLE, library, packageName, "footprint");
    }

    /** The code a converted symbol is named by, and every component drawn with it names it by: {@code SPARKFUN-RESISTORS:RESISTOR:S}. */
    static String symbolCode(String library, String symbol) {
        return ImportedNames.code(TechnicalClass.SYMBOL, library, symbol);
    }

    /** The code a converted package is named by: {@code SPARKFUN-RESISTORS:0603:F}. */
    static String footprintCode(String library, String packageName) {
        return ImportedNames.code(TechnicalClass.FOOTPRINT, library, packageName);
    }

    /** How a converted drawing's source begins, by what it was in Eagle: then its library, a colon, and its name there. */
    private static final String SYMBOL_SOURCE = "Eagle symbol ";
    private static final String PACKAGE_SOURCE = "Eagle package ";
    private static final String DEVICE_SOURCE = "Eagle device ";

    /** The Eagle library a converted drawing comes from, as its source says it — {@code SparkFun-Resistors} —; empty for none. */
    static Optional<String> libraryOf(String source) {
        if (source == null) {
            return Optional.empty();
        }
        return Stream.of(SYMBOL_SOURCE, PACKAGE_SOURCE, DEVICE_SOURCE).filter(source::startsWith).findFirst()
                .map(prefix -> source.substring(prefix.length()))
                .filter(written -> written.indexOf(':') > 0)
                .map(written -> written.substring(0, written.indexOf(':')));
    }

    /** What a converted symbol writes as its source: {@code Eagle symbol SparkFun-Resistors:RESISTOR}. */
    static String symbolSource(String library, String symbol) {
        return SYMBOL_SOURCE + library + ":" + symbol;
    }

    static String packageSource(String library, String packageName) {
        return PACKAGE_SOURCE + library + ":" + packageName;
    }

    static String deviceSource(String library, String deviceSet, String device) {
        return DEVICE_SOURCE + library + ":" + deviceSet + (device.isEmpty() ? "" : ":" + device);
    }

    /**
     * What each pin of a symbol is called in jM, by its Eagle name: the identifier every import makes of a name
     * ({@link ImportedNames#name}) — {@code GND@1} is {@code GND_1}, {@code IN+} is {@code IN_plus}, {@code 1} is
     * {@code pin1} —, two that come out alike told apart by a count, in the order the symbol has them.
     */
    static Map<String, String> pins(EagleLibrary.Symbol symbol) {
        return pins(symbol, "");
    }

    /** The pins of one gate of a part drawn as a box: each opens with its gate's name, {@code A_OUT}, {@code B_OUT}. */
    static Map<String, String> pins(EagleLibrary.Symbol symbol, String gate) {
        Map<String, String> names = new LinkedHashMap<>();
        UniqueIdentifiers taken = new UniqueIdentifiers(PIN, UniqueIdentifiers.UNLIMITED);
        symbol.pins().forEach(pin -> names.put(pin.name(), taken.of(gate.isEmpty() ? pin.name() : gate + "_" + pin.name())));
        return names;
    }

    /**
     * The number each pad of a package carries in jM, by its Eagle name: a pad numbered as a package is — {@code 1},
     * {@code 14} — keeps its number; one Eagle names otherwise — {@code P$1}, {@code A3}, {@code GND} — is numbered after
     * the highest of those, in the order the package has them. A pin's numbers are digits, so both the footprint and the
     * component landing on it number its pads by this.
     */
    static Map<String, String> pads(EagleLibrary.Package found) {
        List<String> names = new ArrayList<>();
        found.pads().forEach(pad -> names.add(pad.name()));
        found.smds().forEach(smd -> names.add(smd.name()));
        long highest = names.stream().filter(ImportedPins::numbered).mapToLong(Long::parseLong).max().orElse(0);
        Map<String, String> numbers = new LinkedHashMap<>();
        for (String name : names) {
            if (ImportedPins.numbered(name)) {
                numbers.put(name, name);
            } else if (!numbers.containsKey(name)) {
                numbers.put(name, String.valueOf(++highest));
            }
        }
        return numbers;
    }
}
