package org.jmouse.cad.libre.pcb;

import org.jmouse.cad.library.CadLanguage;
import org.jmouse.libre.pcb.LibrePcbGraphic;
import org.jmouse.libre.pcb.LibrePcbLibrary;
import org.jmouse.libre.pcb.LibrePcbPoint;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** A LibrePCB footprint's {{NAME}} and {{VALUE}} become its fields, their middle found from the alignment LibrePCB gives. */
class LibrePcbFootprintFieldsTest {

    @Test
    void theNameAndValueStandByTheirMiddleAndOtherWordsAreNoFields() {
        LibrePcbLibrary.Footprint footprint = new LibrePcbLibrary.Footprint("f", "default", List.of(), List.of(
                // Centred over the part, standing on its baseline at y = 1.5: its middle half a 1 mm height higher.
                new LibrePcbGraphic.Text("top_names", 1, new LibrePcbPoint(0, 1.5), 0, LibrePcbGraphic.Alignment.CENTRE,
                        LibrePcbGraphic.Across.BOTTOM, false, "{{NAME}}"),
                // Hanging from y = -1.5 by its top: its middle half a 0.8 mm height lower.
                new LibrePcbGraphic.Text("top_values", 0.8, new LibrePcbPoint(0, -1.5), 0, LibrePcbGraphic.Alignment.CENTRE,
                        LibrePcbGraphic.Across.TOP, false, "{{VALUE}}"),
                new LibrePcbGraphic.Text("top_documentation", 0.5, new LibrePcbPoint(0, 0), 0, LibrePcbGraphic.Alignment.CENTRE,
                        LibrePcbGraphic.Across.MIDDLE, false, "{{NAME}} here")),
                List.of(), List.of(), new LibrePcbLibrary.Axes(0, 0, 0), new LibrePcbLibrary.Axes(0, 0, 0));
        LibrePcbLibrary.Package found = new LibrePcbLibrary.Package("p", "R0805", "", "", Map.of(), Map.of(), List.of(footprint));
        String written = CadLanguage.standard().language().write(
                LibrePcbFootprintDrawing.documentOf(found, footprint, Optional.empty(), LibrePcbFiling.standard()));
        assertTrue(written.contains("field(reference, at: 0 -2, size: 1)"), written);
        assertTrue(written.contains("field(value, at: 0 1.9, size: 0.8)"), written);
        assertTrue(written.split("field\\(").length == 3, written);
    }
}
