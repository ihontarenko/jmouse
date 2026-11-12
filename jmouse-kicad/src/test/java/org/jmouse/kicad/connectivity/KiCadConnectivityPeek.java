package org.jmouse.kicad.connectivity;

import org.jmouse.kicad.KiCadProject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.nio.file.Path;

/** A design on disk, its nets as this model joins them, printed for the parts asked: what to hold against KiCad's own netlist. */
@EnabledIfSystemProperty(named = "kicad.peek.schematic", matches = ".+")
class KiCadConnectivityPeek {
    @Test
    void peek() {
        KiCadProject project = KiCadProject.load(Path.of(System.getProperty("kicad.peek.schematic")));
        String asked = System.getProperty("kicad.peek.parts", "");
        for (KiCadNet net : KiCadConnectivity.of(project).nets()) {
            String pins = String.join(" ", net.pins().stream().map(pin -> pin.reference() + "." + pin.pin().number() + "@" + pin.sheetPath().length()).toList());
            if (net.pins().stream().anyMatch(pin -> (" " + asked + " ").contains(" " + pin.reference() + "." + pin.pin().number() + " "))) {
                System.out.println("NET " + net.name() + " : " + pins);
            }
        }
    }
}
