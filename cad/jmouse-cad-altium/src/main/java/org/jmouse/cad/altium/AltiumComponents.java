package org.jmouse.cad.altium;

import org.jmouse.cad.exchange.imported.NamedFootprint;
import org.jmouse.altium.AltiumSchematic;
import org.jmouse.altium.AltiumSchematic.Component;
import org.jmouse.cad.component.Pin;
import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.exchange.imported.ImportedComponent;
import org.jmouse.cad.exchange.imported.ImportedNames;
import org.jmouse.cad.exchange.imported.ImportedPins;
import org.jmouse.cad.exchange.imported.ImportedSummary;
import org.jmouse.cad.simulation.SimulationVocabulary;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * The component an Altium part stands for: its pins, each named by its package designator ({@code pin1}, {@code pinA3})
 * as every import names a pin and numbered by it where that is a number, the symbol it is drawn with, the footprint's
 * name — a part that can be placed, wired and stored, and that computes nothing until its model is written.
 *
 * <p>A part of one piece keeps the drawing it came with. A part placed as several — a quad gate, an op-amp and its power
 * — is our box, its pins those of every piece the sheet places: a part is drawn with one symbol.</p>
 */
final class AltiumComponents {

    private AltiumComponents() {
    }

    /**
     * @param pieces    the records placing the part: one, or each piece of a part of several
     * @param footprint the footprint the part names, as Altium writes it and as it was matched where the import is read; empty
     *                  for none
     */
    static DocumentNode documentOf(List<Component> pieces, Optional<NamedFootprint> footprint, AltiumLibraries filing) {
        Component first = pieces.getFirst();
        Set<String> bubbled = new LinkedHashSet<>();
        Map<String, Pin> pins = new LinkedHashMap<>();
        for (Component piece : pieces) {
            for (AltiumSchematic.Pin pin : piece.pins()) {
                String name = ImportedPins.nameOf(pin.designator());
                if (pin.inverted()) {
                    bubbled.add(name);
                }
                List<List<String>> numbers = ImportedPins.numbered(pin.designator()) ? List.of(List.of(pin.designator())) : List.of();
                pins.putIfAbsent(name, new Pin(name, null, natureOf(pin), directionOf(pin), null, numbers,
                        pin.clocked(), pin.name()));
            }
        }
        String reference = first.libraryReference();
        ImportedComponent.Drawing drawing = first.multiPart() ? new ImportedComponent.Drawing.Box()
                : new ImportedComponent.Drawing.Uses(AltiumNames.symbolCode(first.sourceLibrary(), reference));
        String said = first.description().isBlank() ? reference : ImportedSummary.shortened(first.description());
        return new ImportedComponent(reference, AltiumNames.component(reference),
                said + " — imported from Altium: its pins and its drawing, with no model yet",
                filing.provenance(AltiumNames.componentSource(first.sourceLibrary(), reference), false),
                ImportedNames.designatorOf(first.designator()).orElse(null), footprint.map(NamedFootprint::written).orElse(null),
                new ArrayList<>(pins.values()), bubbled, drawing, footprint.flatMap(NamedFootprint::code).orElse(null)).document(filing);
    }

    /** A pin's nature, as Altium's electrical type for it and its name say ({@link ImportedPins}). */
    private static String natureOf(AltiumSchematic.Pin pin) {
        ImportedPins.Role role = switch (pin.electrical()) {
            case POWER -> ImportedPins.Role.POWER;
            case PASSIVE -> ImportedPins.Role.PASSIVE;
            case INPUT, BIDIRECTIONAL, OUTPUT, OPEN_COLLECTOR, HIGH_IMPEDANCE, OPEN_EMITTER -> ImportedPins.Role.SIGNAL;
        };
        return ImportedPins.natureOf(pin.name(), role);
    }

    private static String directionOf(AltiumSchematic.Pin pin) {
        return switch (pin.electrical()) {
            // An open collector, an open emitter and a three-state pin drive their line or let it go: outputs.
            case OUTPUT, OPEN_COLLECTOR, OPEN_EMITTER, HIGH_IMPEDANCE -> SimulationVocabulary.OUTPUT;
            case BIDIRECTIONAL -> SimulationVocabulary.BIDIRECTIONAL;
            case INPUT, PASSIVE, POWER -> SimulationVocabulary.INPUT;
        };
    }
}
