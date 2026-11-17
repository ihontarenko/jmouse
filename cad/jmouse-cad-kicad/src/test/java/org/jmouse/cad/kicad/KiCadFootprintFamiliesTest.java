package org.jmouse.cad.kicad;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** A KiCad footprint lands on this library's footprint of the same sizes, by the families the shipped map writes once. */
class KiCadFootprintFamiliesTest {

    private final KiCadLibraryMap map = KiCadLibraryMap.shipped();

    @Test
    void theFootprintsOfARealBoardFindOursBySize() {
        Map<String, String> expected = Map.ofEntries(
                Map.entry("Resistor_THT:R_Axial_DIN0207_L6.3mm_D2.5mm_P10.16mm_Horizontal", "AXIAL:R0207-P10.16-H:F"),
                Map.entry("Resistor_THT:R_Axial_DIN0617_L17.0mm_D6.0mm_P20.32mm_Horizontal", "AXIAL:R0617-P20.32-H:F"),
                Map.entry("Diode_THT:D_DO-201AD_P15.24mm_Horizontal", "AXIAL:DO-201AD-P15.24-H:F"),
                Map.entry("Diode_THT:D_DO-35_SOD27_P7.62mm_Horizontal", "AXIAL:DO-35-P7.62-H:F"),
                Map.entry("Capacitor_THT:CP_Radial_D10.0mm_P5.00mm", "RADIAL:CP-D10-P5:F"),
                Map.entry("Capacitor_THT:C_Disc_D4.3mm_W1.9mm_P5.00mm", "RADIAL:CD-4.3X1.9-P5:F"),
                Map.entry("Capacitor_THT:C_Rect_L7.2mm_W5.5mm_P5.00mm_FKS2_FKP2_MKS2_MKP2", "RADIAL:CB-7.2X5.5-P5:F"),
                Map.entry("Capacitor_THT:C_Rect_L28.0mm_W12.0mm_P22.50mm_MKS4", "RADIAL:CB-28X12-P22.5:F"),
                Map.entry("Package_DIP:DIP-8_W7.62mm", "DIP:8-W7.62:F"),
                Map.entry("Package_SO:TSSOP-8_4.4x3mm_P0.65mm", "TSSOP:8-P0.65-W4.4:F"),
                Map.entry("Package_SO:SOIC-14_3.9x8.7mm_P1.27mm", "SOIC:14-P1.27-W3.9:F"),
                Map.entry("Package_SO:SOIC-16W_7.5x10.3mm_P1.27mm", "SOIC:16-P1.27-W7.5:F"),
                Map.entry("Package_SO:SOIC-8W_5.3x5.3mm_P1.27mm", "SOIC:8-P1.27-W5.3:F"),
                Map.entry("Package_TO_SOT_THT:TO-220-3_Vertical", "TO220:3-V:F"),
                Map.entry("Connector_PinHeader_2.54mm:PinHeader_2x05_P2.54mm_Vertical", "HDR:2X5-P2.54-V:F"),
                Map.entry("MountingHole:MountingHole_3.2mm_M3_Pad", "MH:M3-PAD:F"),
                Map.entry("MountingHole:MountingHole_3.2mm_M3", "MH:M3:F"),
                Map.entry("Resistor_SMD:R_2512_6332Metric", "CHIP:R2512:F"));
        expected.forEach((kicad, ours) -> assertEquals(Optional.of(ours), map.footprintCodeOf(kicad), kicad));
    }

    @Test
    void aSizeOrAVariantOfAnotherShapeIsLeftToKiCad() {
        assertEquals(Optional.empty(), map.footprintCodeOf("Capacitor_THT:CP_Radial_D35.0mm_P10.00mm_SnapIn"));
        assertEquals(Optional.empty(), map.footprintCodeOf("Resistor_SMD:R_0805_2012Metric_Pad1.20x1.40mm_HandSolder"));
        assertEquals(Optional.empty(), map.footprintCodeOf("Package_DIP:DIP-8_W7.62mm_Socket"));
    }

    @Test
    void anIdWhoseSizeIsNoNumberIsOfAnotherFamily() {
        FootprintFamily counted = FootprintFamily.of("Lib:SO-{leads}_P{pitch}mm", "SO:{leads|number}-P{pitch|number}:F");
        assertEquals(Optional.empty(), counted.codeOf("Lib:SO-8W_P1.27mm"));
        assertEquals(Optional.of("SO:8-P1.27:F"), counted.codeOf("Lib:SO-8_P1.27mm"));
    }

    @Test
    void anExactEntryIsTheWayBackAndAFamilyIsNot() {
        assertEquals(Optional.of("Resistor_SMD:R_0805_2012Metric"), map.footprintIdOf("CHIP:R0805:F"));
        assertEquals(Optional.empty(), map.footprintIdOf("AXIAL:R0207-P10.16-H:F"));
    }

    @Test
    void aCodeWritingWhatItsIdDoesNotReadIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> FootprintFamily.of("Lib:X_{size}", "X:{pitch}:F"));
    }
}
