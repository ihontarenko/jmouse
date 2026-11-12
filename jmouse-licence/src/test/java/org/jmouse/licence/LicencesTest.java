package org.jmouse.licence;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LicencesTest {

    private static final Licences LICENCES = Licences.standard();

    private static final String KICAD = "CC-BY-SA-4.0 WITH KiCad-libraries-exception";

    @Test
    void anExpressionIsReadWithItsDashesDotsAndOperators() {
        LicenceExpression read = LicenceExpression.read(KICAD);
        assertEquals(new LicenceExpression.Single("CC-BY-SA-4.0", false, Optional.of("KiCad-libraries-exception")), read);

        LicenceExpression grouped = LicenceExpression.read("(MIT OR Apache-2.0) AND GPL-2.0+");
        LicenceExpression.AllOf all = assertInstanceOf(LicenceExpression.AllOf.class, grouped);
        assertInstanceOf(LicenceExpression.AnyOf.class, all.parts().getFirst());
        assertEquals(new LicenceExpression.Single("GPL-2.0", true, Optional.empty()), all.parts().get(1));
        assertEquals("(MIT OR Apache-2.0) AND GPL-2.0+", grouped.written());

        // AND binds before OR, as SPDX says.
        LicenceExpression.AnyOf any = assertInstanceOf(LicenceExpression.AnyOf.class, LicenceExpression.read("MIT or CC0-1.0 and GPL-3.0-only"));
        assertEquals(2, any.parts().size());
        assertInstanceOf(LicenceExpression.AllOf.class, any.parts().get(1));
    }

    @Test
    void aTextThatIsNoExpressionIsRefusedSayingWhy() {
        assertThrows(IllegalArgumentException.class, () -> LicenceExpression.read("MIT AND"));
        assertThrows(IllegalArgumentException.class, () -> LicenceExpression.read("(MIT OR CC0-1.0"));
        assertThrows(IllegalArgumentException.class, () -> LicenceExpression.read("MIT CC0-1.0"));
        assertThrows(IllegalArgumentException.class, () -> LicenceExpression.read("WITH MIT"));
    }

    @Test
    void kicadLibrariesAreFreeInsideADesignAndKeepTheirLicenceAsThemselves() {
        assertEquals(Verdict.FREE, LICENCES.rule(KICAD, LicenceUse.EMBEDDED).verdict());
        assertEquals(Verdict.SAME_TERMS, LICENCES.rule(KICAD, LicenceUse.REDISTRIBUTED).verdict());
        LicenceRuling adopted = LICENCES.rule(KICAD, LicenceUse.ADOPTED);
        assertEquals(Verdict.REFUSED, adopted.verdict());
        assertTrue(adopted.reason().contains("draw your own"));
    }

    @Test
    void shareAlikeReachesWhatAWorkIsBuiltIntoOnlyWhenStrong() {
        assertEquals(Verdict.SAME_TERMS, LICENCES.rule("CC-BY-SA-4.0", LicenceUse.EMBEDDED).verdict());
        assertEquals(Verdict.ATTRIBUTED, LICENCES.rule("CERN-OHL-W-2.0", LicenceUse.EMBEDDED).verdict());
        assertEquals(Verdict.REFUSED, LICENCES.rule("CERN-OHL-W-2.0", LicenceUse.ADOPTED).verdict());
    }

    @Test
    void permissiveLicencesOweCreditAndPublicDomainNothing() {
        assertEquals(Verdict.ATTRIBUTED, LICENCES.rule("MIT", LicenceUse.ADOPTED).verdict());
        assertEquals(Verdict.FREE, LICENCES.rule("CC0-1.0", LicenceUse.ADOPTED).verdict());
        assertTrue(LICENCES.rule("cc0-1.0", LicenceUse.REDISTRIBUTED).permits());
    }

    @Test
    void allOfOwesTheStrictestAndAnyOfTheLightest() {
        assertEquals(Verdict.SAME_TERMS, LICENCES.rule("MIT AND CC-BY-SA-4.0", LicenceUse.EMBEDDED).verdict());
        assertEquals(Verdict.ATTRIBUTED, LICENCES.rule("MIT OR CC-BY-SA-4.0", LicenceUse.EMBEDDED).verdict());
        assertEquals(Verdict.FREE, LICENCES.rule("GPL-3.0-only OR CC0-1.0", LicenceUse.ADOPTED).verdict());
    }

    @Test
    void aWorkOfUnknownOrUnstatedTermsTravelsOnlyInsideWhatItCameWith() {
        for (String expression : List.of("", "LicenseRef-vendor-proprietary")) {
            assertEquals(Verdict.ATTRIBUTED, LICENCES.rule(expression, LicenceUse.EMBEDDED).verdict());
            assertFalse(LICENCES.rule(expression, LicenceUse.REDISTRIBUTED).permits());
            assertFalse(LICENCES.rule(expression, LicenceUse.ADOPTED).permits());
        }
        assertTrue(LICENCES.rule(null, LicenceUse.EMBEDDED).reason().startsWith("No licence is stated"));
    }

    @Test
    void anUnknownExceptionWaivesNothing() {
        LicenceRuling ruled = LICENCES.rule("CC-BY-SA-4.0 WITH Vendor-exception", LicenceUse.EMBEDDED);
        assertEquals(Verdict.SAME_TERMS, ruled.verdict());
        assertTrue(ruled.reason().contains("Vendor-exception"));
    }

    @Test
    void aLicencesTextIsLinkedBySpdx() {
        assertEquals("https://spdx.org/licenses/MIT.html", LICENCES.home("MIT"));
        assertEquals("CERN Open Hardware Licence Version 2 - Weakly Reciprocal", LICENCES.terms("cern-ohl-w-2.0").orElseThrow().title());
    }
}
