package org.jmouse.cad.library;

import org.jmouse.cad.component.Seat;
import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.pcb.PcbModule;
import org.jmouse.cad.pcb.drawing.FootprintDrawing;
import org.jmouse.cad.simulation.ComponentFacts;
import org.jmouse.cad.simulation.SimulationModule;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Every component the library ships lands on every footprint it names: the footprint is the library's, and each pin
 * takes a pad — by the package number the pin carries, or by the component's pad map for that footprint, which names
 * only pads the footprint has and pins the component has, and leaves no pin without a pad. A pad the map leaves out
 * is one the part does not use (an EI transformer's spare pins).
 */
class ComponentSeatsTest {

    @Test
    void everyComponentLandsOnEveryFootprintItNames() {
        CadLibrary library = CadLibrary.library();
        ComponentFacts facts = new ComponentFacts(library);
        List<String> wrong = new ArrayList<>();
        for (String slug : library.slugs()) {
            DocumentNode document = library.find(slug).orElseThrow();
            if (!document.kind().equals(SimulationModule.COMPONENT)) {
                continue;
            }
            Map<String, String> pinsByNumber = facts.pinsByNumber(slug);
            for (Seat seat : facts.seatsOf(slug)) {
                Optional<DocumentNode> footprint = library.find(seat.footprint()).filter(found -> found.kind().equals(PcbModule.FOOTPRINT));
                if (footprint.isEmpty()) {
                    wrong.add(slug + " names the footprint " + seat.footprint() + ", which the library does not hold");
                    continue;
                }
                Set<String> pads = new HashSet<>();
                FootprintDrawing.of(footprint.get()).pads().forEach(pad -> pads.add(pad.number()));
                if (seat.pads().isEmpty()) {
                    pinsByNumber.forEach((number, pin) -> {
                        if (!pads.contains(number)) {
                            wrong.add(slug + " on " + seat.footprint() + ": its pin " + pin + " is number " + number + ", which is no pad");
                        }
                    });
                    continue;
                }
                Set<String> mapped = new HashSet<>(seat.pads().values());
                seat.pads().forEach((pad, pin) -> {
                    if (!pads.contains(pad)) {
                        wrong.add(slug + " on " + seat.footprint() + ": the map names the pad " + pad + ", which the footprint lacks");
                    }
                    if (!pinsByNumber.containsValue(pin)) {
                        wrong.add(slug + " on " + seat.footprint() + ": the map names the pin " + pin + ", which the component lacks");
                    }
                });
                pinsByNumber.values().stream().filter(pin -> !mapped.contains(pin))
                        .forEach(pin -> wrong.add(slug + " on " + seat.footprint() + ": its pin " + pin + " takes no pad"));
            }
        }
        assertEquals(List.of(), wrong);
    }
}
