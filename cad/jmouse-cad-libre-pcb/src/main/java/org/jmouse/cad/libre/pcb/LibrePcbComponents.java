package org.jmouse.cad.libre.pcb;

import org.jmouse.cad.component.Pin;
import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.exchange.imported.ImportedCircuit;
import org.jmouse.cad.exchange.imported.ImportedComponent;
import org.jmouse.cad.exchange.imported.ImportedNames;
import org.jmouse.cad.exchange.imported.ImportedPins;
import org.jmouse.cad.exchange.imported.UniqueIdentifiers;
import org.jmouse.cad.simulation.SimulationVocabulary;
import org.jmouse.libre.pcb.LibrePcbLibrary;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * A component of the library, in the variant and the device a part is, as a jM component. A variant of one gate with pins
 * keeps the drawing it came with, its pins named as its symbol's are. One of several — a dual amplifier — is our box, its
 * pins named by the signals they carry: a part is drawn with one symbol.
 */
final class LibrePcbComponents {

    private static final String PIN_OPENING = "P";

    /** How a part of a component is drawn: by one of its gates, or as our box. */
    sealed interface Drawn {

        record ByGate(LibrePcbLibrary.Gate gate, LibrePcbLibrary.Symbol symbol) implements Drawn {
        }

        record AsBox() implements Drawn {
        }
    }

    private LibrePcbComponents() {
    }

    static Drawn drawnOf(LibrePcbLibrary library, LibrePcbLibrary.Variant variant) {
        List<LibrePcbLibrary.Gate> pinned = variant.gates().stream().filter(gate -> !symbolOf(library, gate).pins().isEmpty()).toList();
        if (pinned.size() > 1) {
            return new Drawn.AsBox();
        }
        LibrePcbLibrary.Gate gate = pinned.isEmpty() ? variant.gates().getFirst() : pinned.getFirst();
        return new Drawn.ByGate(gate, symbolOf(library, gate));
    }

    /** The symbol a gate is drawn with, which the project's library must carry. */
    static LibrePcbLibrary.Symbol symbolOf(LibrePcbLibrary library, LibrePcbLibrary.Gate gate) {
        LibrePcbLibrary.Symbol symbol = library.symbols().get(gate.symbol());
        if (symbol == null) {
            throw new IllegalArgumentException("A gate is drawn with the symbol " + gate.symbol() + ", which the project's library does not carry.");
        }
        return symbol;
    }

    /** What each signal is called on the component: by its pin's name for a part drawn by a gate, by its own name in a box. */
    static Map<String, String> pinOfSignal(LibrePcbLibrary.Component component, Drawn drawn) {
        Map<String, String> pins = new LinkedHashMap<>();
        switch (drawn) {
            case Drawn.ByGate byGate -> {
                Map<String, String> names = LibrePcbSymbolDrawing.pinNames(byGate.symbol());
                byGate.gate().signals().forEach((pin, signal) -> pins.put(signal, names.get(pin)));
            }
            case Drawn.AsBox ignored -> {
                UniqueIdentifiers unique = new UniqueIdentifiers(PIN_OPENING, ImportedCircuit.ID_LONGEST);
                component.signals().forEach(signal -> pins.put(signal.id(), unique.of(ImportedNames.name(signal.name(), PIN_OPENING))));
            }
        }
        return pins;
    }

    /**
     * @param device       the device the part is; empty for a part with no package
     * @param footprint    the footprint it lands on, by slug; null for none
     * @param slug         what the component is called
     */
    // A pin's numbers are digits as the language writes them; a package's pads named otherwise land through the part's pads.
    static DocumentNode documentOf(LibrePcbLibrary library, LibrePcbLibrary.Component component, Drawn drawn, Optional<LibrePcbLibrary.Device> device,
                                   String footprint, String slug, LibrePcbFiling filing) {
        Set<String> bubbled = new LinkedHashSet<>();
        List<Pin> pins = pinsOf(library, component, drawn, device, bubbled);
        ImportedComponent.Drawing drawing = switch (drawn) {
            case Drawn.ByGate byGate -> new ImportedComponent.Drawing.Uses(LibrePcbNames.symbolCode(byGate.symbol()));
            case Drawn.AsBox ignored -> new ImportedComponent.Drawing.Box();
        };
        String said = component.description().isBlank() ? component.name() : component.description().strip().lines().findFirst().orElse(component.name());
        String packageName = device.map(LibrePcbLibrary.Device::packageId).map(library.packages()::get).map(LibrePcbLibrary.Package::name).orElse(null);
        return new ImportedComponent(device.map(LibrePcbLibrary.Device::name).orElse(component.name()), slug,
                said + " — imported from LibrePCB: its pins and its drawing, with no model yet",
                filing.provenance(LibrePcbNames.source("component", component.name(), component.id()), component.author()),
                ImportedNames.designatorOf(component.prefix()).orElse(null), packageName, pins, bubbled, drawing, footprint).document(filing);
    }

    private static List<Pin> pinsOf(LibrePcbLibrary library, LibrePcbLibrary.Component component, Drawn drawn, Optional<LibrePcbLibrary.Device> device,
                                    Set<String> bubbled) {
        Map<String, String> pinOfSignal = pinOfSignal(component, drawn);
        List<Pin> pins = new ArrayList<>();
        for (LibrePcbLibrary.Signal signal : component.signals()) {
            String name = pinOfSignal.get(signal.id());
            if (name == null) {
                continue;
            }
            if (signal.negated()) {
                bubbled.add(name);
            }
            List<String> numbers = device.map(found -> padsOf(library, found, signal.id())).orElse(List.of());
            pins.add(new Pin(name, null, natureOf(signal), directionOf(signal), null, numbers.isEmpty() ? List.of() : List.of(numbers), signal.clock(), signal.name()));
        }
        // A pin of the drawing that carries no signal is still drawn: it is on no pad and no net.
        if (drawn instanceof Drawn.ByGate byGate) {
            Map<String, String> names = LibrePcbSymbolDrawing.pinNames(byGate.symbol());
            for (LibrePcbLibrary.Pin pin : byGate.symbol().pins()) {
                if (!byGate.gate().signals().containsKey(pin.id())) {
                    pins.add(new Pin(names.get(pin.id()), null, ImportedPins.natureOf(pin.name(), ImportedPins.Role.SIGNAL), SimulationVocabulary.INPUT,
                            null, List.of(), false, pin.name()));
                }
            }
        }
        return pins;
    }

    /** The names of the pads of a device's package that carry a signal. */
    private static List<String> padsOf(LibrePcbLibrary library, LibrePcbLibrary.Device device, String signal) {
        LibrePcbLibrary.Package found = library.packages().get(device.packageId());
        if (found == null) {
            throw new IllegalArgumentException("The device " + device.name() + " comes in a package the project's library does not carry.");
        }
        return device.padSignals().entrySet().stream().filter(pad -> pad.getValue().equals(signal))
                .map(pad -> found.padNames().get(pad.getKey())).filter(name -> name != null).filter(ImportedPins::numbered).sorted().toList();
    }

    /**
     * The pin each of a device's pads takes, by the pad's name, where the package names any of them by something other than
     * digits — {@code A}, {@code A1} —; none where a pin's own numbers say it all.
     */
    static Map<String, String> padsOf(LibrePcbLibrary library, LibrePcbLibrary.Device device, Map<String, String> pinOfSignal) {
        LibrePcbLibrary.Package found = library.packages().get(device.packageId());
        if (found == null || found.padNames().values().stream().allMatch(ImportedPins::numbered)) {
            return Map.of();
        }
        Map<String, String> pads = new LinkedHashMap<>();
        device.padSignals().forEach((pad, signal) -> {
            String name = found.padNames().get(pad);
            String pin = pinOfSignal.get(signal);
            if (name != null && pin != null) {
                pads.put(name, pin);
            }
        });
        return pads;
    }

    private static String natureOf(LibrePcbLibrary.Signal signal) {
        ImportedPins.Role role = switch (signal.role()) {
            case POWER -> ImportedPins.Role.POWER;
            case PASSIVE -> ImportedPins.Role.PASSIVE;
            case INPUT, OUTPUT, BIDIRECTIONAL, OPEN_DRAIN -> ImportedPins.Role.SIGNAL;
        };
        return ImportedPins.natureOf(signal.name(), role);
    }

    private static String directionOf(LibrePcbLibrary.Signal signal) {
        return switch (signal.role()) {
            // An open drain drives its line or lets it go: an output, never one that reads.
            case OUTPUT, OPEN_DRAIN -> SimulationVocabulary.OUTPUT;
            case BIDIRECTIONAL -> SimulationVocabulary.BIDIRECTIONAL;
            case INPUT, POWER, PASSIVE -> SimulationVocabulary.INPUT;
        };
    }
}
