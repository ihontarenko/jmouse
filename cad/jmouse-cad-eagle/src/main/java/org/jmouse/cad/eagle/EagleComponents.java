package org.jmouse.cad.eagle;

import org.jmouse.cad.component.Pin;
import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.exchange.imported.ImportedComponent;
import org.jmouse.cad.exchange.imported.ImportedNames;
import org.jmouse.cad.exchange.imported.ImportedPins;
import org.jmouse.cad.exchange.imported.ImportedSummary;
import org.jmouse.cad.simulation.SimulationVocabulary;
import org.jmouse.eagle.EagleLibrary;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * The component an Eagle device stands for: its gates' pins, each numbered by the pads Eagle connects it to, the symbol
 * it is drawn with, the footprint its package lands on — a part that can be placed, wired and stored, and that computes
 * nothing until its model is written.
 *
 * <p>A device of one gate keeps the drawing it came with, its pins named as its symbol's are. A device of several — a
 * dual op-amp, a quad gate — is our box, its pins placed by what they are and named with their gate's
 * ({@code A_OUT}): a part is drawn with one symbol.</p>
 */
final class EagleComponents {

    /** How a part of a device set is drawn: by one of its gates, or as our box. */
    sealed interface Drawn {

        /** By this gate's symbol: its one gate with pins, or its first where none has any — a frame, a mounting hole. */
        record ByGate(EagleLibrary.Gate gate) implements Drawn {
        }

        /** As our box, its pins placed on its sides by what they are: several of its gates have pins. */
        record AsBox() implements Drawn {
        }
    }

    private EagleComponents() {
    }

    /** How a part of the device set is drawn: a part is drawn with one symbol, so one with several gates of pins is our box. */
    static Drawn drawnOf(EagleLibrary library, EagleLibrary.DeviceSet deviceSet) {
        List<EagleLibrary.Gate> pinned = deviceSet.gates().stream()
                .filter(gate -> !symbolOf(library, gate).pins().isEmpty()).toList();
        return switch (pinned.size()) {
            case 0 -> new Drawn.ByGate(deviceSet.gates().getFirst());
            case 1 -> new Drawn.ByGate(pinned.getFirst());
            default -> new Drawn.AsBox();
        };
    }

    static DocumentNode documentOf(EagleLibrary library, EagleLibrary.DeviceSet deviceSet, EagleLibrary.Device device,
                                   EagleLibraries filing) {
        Set<String> bubbled = new LinkedHashSet<>();
        List<Pin> pins = pinsOf(library, deviceSet, device, bubbled);
        ImportedComponent.Drawing drawing = switch (drawnOf(library, deviceSet)) {
            case Drawn.ByGate byGate -> new ImportedComponent.Drawing.Uses(EagleNames.symbolCode(library.name(), byGate.gate().symbol()));
            case Drawn.AsBox ignored -> new ImportedComponent.Drawing.Box();
        };
        String said = deviceSet.description().isBlank() ? EagleNames.deviceSource(library.name(), deviceSet.name(), device.name())
                : ImportedSummary.ofHtml(deviceSet.description());

        String packageName = device.packageName();
        return new ImportedComponent(deviceSet.name() + device.name(),
                EagleNames.component(library.name(), deviceSet.name(), device.name()),
                said + " — imported from Eagle: its pins and its drawing, with no model yet",
                filing.provenance(EagleNames.deviceSource(library.name(), deviceSet.name(), device.name()), false),
                ImportedNames.designatorOf(deviceSet.prefix()).orElse(null), packageName, pins, bubbled, drawing,
                packageName == null ? null : EagleNames.footprintCode(library.name(), packageName)).document(filing);
    }

    /**
     * What each pin of each gate is called on the component, by gate and Eagle pin: its symbol's own names for a part
     * drawn by a gate, names opening with their gate's for one drawn as our box — what a sheet's pin references connect.
     */
    static Map<String, Map<String, String>> pinNames(EagleLibrary library, EagleLibrary.DeviceSet deviceSet) {
        Map<String, Map<String, String>> names = new LinkedHashMap<>();
        boolean boxed = drawnOf(library, deviceSet) instanceof Drawn.AsBox;
        for (EagleLibrary.Gate gate : deviceSet.gates()) {
            EagleLibrary.Symbol symbol = symbolOf(library, gate);
            names.put(gate.name(), boxed ? EagleNames.pins(symbol, gate.name()) : EagleNames.pins(symbol));
        }
        return names;
    }

    /** The symbol a gate is drawn with, which its library must carry. */
    static EagleLibrary.Symbol symbolOf(EagleLibrary library, EagleLibrary.Gate gate) {
        EagleLibrary.Symbol symbol = library.symbols().get(gate.symbol());
        if (symbol == null) {
            throw new IllegalArgumentException("The gate " + gate.name() + " is drawn with the symbol " + gate.symbol()
                    + ", which its library " + library.name() + " does not carry.");
        }
        return symbol;
    }

    /** Every gate's pins, in the gates' order, each numbered by its pads as the package numbers them. */
    private static List<Pin> pinsOf(EagleLibrary library, EagleLibrary.DeviceSet deviceSet, EagleLibrary.Device device,
                                    Set<String> bubbled) {
        Map<String, String> padNumbers = Optional.ofNullable(device.packageName()).map(library.packages()::get)
                .map(EagleNames::pads).orElse(Map.of());
        Map<String, Map<String, String>> names = pinNames(library, deviceSet);
        List<Pin> pins = new ArrayList<>();
        for (EagleLibrary.Gate gate : deviceSet.gates()) {
            for (EagleLibrary.Pin pin : symbolOf(library, gate).pins()) {
                String name = names.get(gate.name()).get(pin.name());
                List<String> numbers = device.connects().stream()
                        .filter(connect -> connect.gate().equals(gate.name()) && connect.pin().equals(pin.name()))
                        .flatMap(connect -> connect.pads().stream()).map(padNumbers::get).filter(number -> number != null)
                        .toList();
                if (pin.function().inverted()) {
                    bubbled.add(name);
                }
                pins.add(new Pin(name, null, natureOf(pin), directionOf(pin), null,
                        numbers.isEmpty() ? List.of() : List.of(numbers), pin.function().clocked(), pin.shownName()));
            }
        }
        return pins;
    }

    /** A pin's nature, as Eagle's direction for it and its name say ({@link ImportedPins}). */
    private static String natureOf(EagleLibrary.Pin pin) {
        ImportedPins.Role role = switch (pin.direction()) {
            case PWR, SUP -> ImportedPins.Role.POWER;
            case PAS -> ImportedPins.Role.PASSIVE;
            case NC, IN, OUT, IO, OC, HIZ -> ImportedPins.Role.SIGNAL;
        };
        return ImportedPins.natureOf(pin.shownName(), role);
    }

    private static String directionOf(EagleLibrary.Pin pin) {
        return switch (pin.direction()) {
            // An open collector and a three-state pin both drive their line or let it go: outputs, never ones that read.
            case OUT, OC, HIZ -> SimulationVocabulary.OUTPUT;
            case IO -> SimulationVocabulary.BIDIRECTIONAL;
            case NC, IN, PWR, PAS, SUP -> SimulationVocabulary.INPUT;
        };
    }
}
