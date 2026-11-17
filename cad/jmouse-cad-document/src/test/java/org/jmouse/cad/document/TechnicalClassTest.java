package org.jmouse.cad.document;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** A code says its class at its end, and its name between its family and its kind. */
class TechnicalClassTest {

    @Test
    void aCodeIsItsDesignationThenItsKind() {
        assertEquals("CHIP:R0805:F", TechnicalClass.FOOTPRINT.code("CHIP:R0805"));
        assertEquals("SOIC:8-P1.27-W3.9:M3D", TechnicalClass.MODEL.code("SOIC:8-P1.27-W3.9"));
    }

    @Test
    void aCodeEndsInItsNameAfterTheFamily() {
        assertEquals(Optional.of("R0805"), TechnicalClass.FOOTPRINT.nameIn("CHIP:R0805:F"));
        assertEquals(Optional.of("8-P1.27-W3.9"), TechnicalClass.FOOTPRINT.nameIn("SOIC:8-P1.27-W3.9:F"));
    }

    @Test
    void anotherClassOrNoFamilyHasNoName() {
        assertEquals(Optional.empty(), TechnicalClass.MODEL.nameIn("CHIP:R0805:F"));
        assertEquals(Optional.empty(), TechnicalClass.FOOTPRINT.nameIn("R0805:F"));
        assertEquals(Optional.empty(), TechnicalClass.FOOTPRINT.nameIn("fp-chip-r0805"));
    }

    @Test
    void aCodeSaysItsClassAndASlugNone() {
        assertEquals(Optional.of(TechnicalClass.SYMBOL), TechnicalClass.ofCode("MARK:GROUND:S"));
        assertEquals(Optional.of(TechnicalClass.MODEL), TechnicalClass.ofCode("SOIC:8:M3D"));
        assertEquals(Optional.of(TechnicalClass.FRAME), TechnicalClass.ofCode("ISO:A4:FRM"));
        assertEquals(Optional.empty(), TechnicalClass.ofCode("chip-resistor-0805-footprint"));
        assertEquals(Optional.empty(), TechnicalClass.ofCode(":F"));
    }

    @Test
    void aKindOfCodedDocumentsSaysItsClassAndAnotherNone() {
        assertEquals(Optional.of(TechnicalClass.FOOTPRINT), TechnicalClass.ofKind("footprint"));
        assertEquals(Optional.of(TechnicalClass.MODEL), TechnicalClass.ofKind("model"));
        assertEquals(Optional.empty(), TechnicalClass.ofKind("circuit"));
    }
}
