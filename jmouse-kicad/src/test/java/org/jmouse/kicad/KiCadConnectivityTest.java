package org.jmouse.kicad;

import org.jmouse.kicad.connectivity.KiCadConnectivity;
import org.jmouse.kicad.connectivity.KiCadNet;
import org.jmouse.kicad.parser.KiCadParser;
import org.jmouse.kicad.schematic.KiCadSchematic;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KiCadConnectivityTest {

    /** A resistor: pin 1 at the top (0, 3.81), pin 2 at the bottom, as KiCad's Device:R draws them. */
    private static final String RESISTOR = """
            (symbol "Device:R" (property "Reference" "R") (property "Value" "R")
              (symbol "R_1_1"
                (pin passive line (at 0 3.81 270) (length 1.27) (name "~") (number "1"))
                (pin passive line (at 0 -3.81 90) (length 1.27) (name "~") (number "2"))))
            (symbol "power:GND" (power) (property "Reference" "#PWR") (property "Value" "GND")
              (symbol "GND_0_1" (pin power_in line (at 0 0 270) (length 0) hide (name "GND") (number "1"))))
            """;

    private static String placed(String reference, double x, double y, int angle, String uuid) {
        return "(symbol (lib_id \"Device:R\") (at " + x + " " + y + " " + angle + ") (unit 1) (uuid \"" + uuid + "\")"
                + " (property \"Reference\" \"" + reference + "\") (property \"Value\" \"1k\"))";
    }

    private static KiCadSchematic schematic(String name, String body) {
        String text = "(kicad_sch (version 20231120) (generator \"eeschema\") (uuid \"" + name + "-uuid\") (lib_symbols "
                + RESISTOR + ") " + body + ")";
        return KiCadSchematic.of(new KiCadParser().parse(name, text));
    }

    @Test
    @DisplayName("a symbol's pins land where KiCad puts them, turned and mirrored")
    void placesPins() {
        KiCadPoint top = new KiCadPoint(0, 3.81);
        KiCadPoint at = new KiCadPoint(100, 50);
        assertPoint(100, 46.19, new KiCadPlacement(at, 0, KiCadPlacement.Mirror.NONE).place(top));
        assertPoint(96.19, 50, new KiCadPlacement(at, 90, KiCadPlacement.Mirror.NONE).place(top));
        assertPoint(100, 53.81, new KiCadPlacement(at, 180, KiCadPlacement.Mirror.NONE).place(top));
        assertPoint(103.81, 50, new KiCadPlacement(at, 270, KiCadPlacement.Mirror.NONE).place(top));
        assertPoint(100, 53.81, new KiCadPlacement(at, 0, KiCadPlacement.Mirror.X).place(top));
        assertPoint(103.81, 50, new KiCadPlacement(at, 90, KiCadPlacement.Mirror.Y).place(top));
        assertThrows(IllegalArgumentException.class, () -> new KiCadPlacement(at, 45, KiCadPlacement.Mirror.NONE));
    }

    @Test
    @DisplayName("a wire joins its ends and what lies on it; crossing wires join only at a junction")
    void joinsOnASheet() {
        // R1 stands upright at (50, 50): its pins at (50, 46.19) and (50, 53.81). R2 likewise at (70, 50).
        String body = placed("R1", 50, 50, 0, "r1") + placed("R2", 70, 50, 0, "r2") + placed("R3", 90, 50, 0, "r3")
                // R1's bottom pin to R2's bottom pin.
                + "(wire (pts (xy 50 53.81) (xy 70 53.81)))"
                // A T: a wire from R3's bottom pin ends in the middle of the one above.
                + "(wire (pts (xy 90 53.81) (xy 90 60) (xy 60 60) (xy 60 53.81)))"
                // R1's top pin across R2's top wire without a junction: no joint.
                + "(wire (pts (xy 50 46.19) (xy 50 40)))"
                + "(wire (pts (xy 40 43) (xy 80 43)))"
                // R2's top pin to ground; R3's top pin left alone, but marked.
                + "(symbol (lib_id \"power:GND\") (at 70 46.19 0) (unit 1) (uuid \"gnd\") (property \"Reference\" \"#PWR01\")"
                + " (property \"Value\" \"GND\"))"
                + "(no_connect (at 90 46.19))";
        KiCadSchematic sheet = schematic("one.kicad_sch", body);
        KiCadConnectivity connectivity = KiCadConnectivity.of(KiCadProject.of("one.kicad_sch", file -> sheet));
        Map<String, List<String>> nets = byName(connectivity);
        assertEquals(List.of("R1-2", "R2-2", "R3-2"), nets.get("Net-(R1-Pad2)"), "the T joins R3");
        assertEquals(List.of("R2-1"), nets.get("GND"), "the power symbol names the net and is not a pin of it");
        assertEquals(List.of("R1-1"), nets.get("unconnected-(R1-Pad1)"), "crossing without a junction joins nothing");
        assertEquals(List.of("R1-1"), connectivity.unconnected().stream().map(Object::toString).toList(),
                "R3's top pin is excused by its mark; R1's is not");
    }

    @Test
    @DisplayName("global labels join across sheets, and a sheet placed by the root is read beside it")
    void joinsAcrossSheets() {
        KiCadSchematic child = schematic("child.kicad_sch", placed("R2", 10, 10, 0, "r2")
                + "(global_label \"VIN\" (at 10 6.19 0))" + "(global_label \"VOUT\" (at 10 13.81 0))");
        KiCadSchematic root = schematic("root.kicad_sch", placed("R1", 10, 10, 0, "r1")
                + "(global_label \"VIN\" (at 10 6.19 0))"
                + "(sheet (at 50 50) (size 20 10) (uuid \"box\") (property \"Sheetname\" \"child\") (property \"Sheetfile\" \"child.kicad_sch\"))");
        KiCadProject project = KiCadProject.of("root.kicad_sch", file -> file.equals("root.kicad_sch") ? root : child);
        assertEquals(List.of("/root.kicad_sch-uuid", "/root.kicad_sch-uuid/box"), project.sheets().stream().map(KiCadProject.SheetInstance::path).toList());
        Map<String, List<String>> nets = byName(KiCadConnectivity.of(project));
        assertEquals(List.of("R1-1", "R2-1"), nets.get("VIN"));
        assertTrue(nets.containsKey("VOUT"));
    }

    @Test
    @DisplayName("a schematic older than KiCad 7 is refused, naming its format")
    void refusesAnOldFormat() {
        IllegalArgumentException refused = assertThrows(IllegalArgumentException.class,
                () -> KiCadSchematic.of(new KiCadParser().parse("old.kicad_sch", "(kicad_sch (version 20211123))")));
        assertTrue(refused.getMessage().contains("20211123"));
    }

    private static Map<String, List<String>> byName(KiCadConnectivity connectivity) {
        return connectivity.nets().stream().collect(java.util.stream.Collectors.toMap(KiCadNet::name,
                net -> net.pins().stream().map(Object::toString).toList()));
    }

    private static void assertPoint(double x, double y, KiCadPoint point) {
        assertTrue(point.sameAs(new KiCadPoint(x, y)), "expected (" + x + ", " + y + "), was " + point);
    }
}
