package org.jmouse.cad.document;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** A code says its class, and ends in the name another product calls the object by without its library. */
class TechnicalClassTest {

    @Test
    void aCodeEndsInItsNameAfterTheNamespace() {
        assertEquals(Optional.of("R0805"), TechnicalClass.FOOTPRINT.nameIn("FP.CHIP:R0805"));
        assertEquals(Optional.of("8-P1.27-W3.9"), TechnicalClass.FOOTPRINT.nameIn("FP.SOIC:8-P1.27-W3.9"));
    }

    @Test
    void anotherClassOrNoNamespaceHasNoName() {
        assertEquals(Optional.empty(), TechnicalClass.MODEL.nameIn("FP.CHIP:R0805"));
        assertEquals(Optional.empty(), TechnicalClass.FOOTPRINT.nameIn("FP.R0805"));
        assertEquals(Optional.empty(), TechnicalClass.FOOTPRINT.nameIn("fp-chip-r0805"));
    }

    @Test
    void aKindOfCodedDocumentsSaysItsClassAndAnotherNone() {
        assertEquals(Optional.of(TechnicalClass.FOOTPRINT), TechnicalClass.ofKind("footprint"));
        assertEquals(Optional.of(TechnicalClass.MODEL), TechnicalClass.ofKind("model"));
        assertEquals(Optional.empty(), TechnicalClass.ofKind("circuit"));
    }
}
