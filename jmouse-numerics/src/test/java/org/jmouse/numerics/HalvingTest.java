package org.jmouse.numerics;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HalvingTest {

    @Test
    void bracketsWhereATestChangesAsCloseAsItsHalvingsReach() {
        Halving.Bracket found = Halving.bracket(0, 1, 20, value -> value <= 0.3);
        assertTrue(found.holds() <= 0.3 && found.fails() > 0.3);
        assertEquals(0.3, found.middle(), 1e-6);
    }

    @Test
    void bracketsFromAboveAsWellAsBelow() {
        Halving.Bracket found = Halving.bracket(1, 0, 20, value -> value >= 0.7);
        assertEquals(0.7, found.middle(), 1e-6);
    }

    @Test
    void keepsWhatWasMadeAtTheFarthestValueThatCouldBeMade() {
        Optional<String> made = Halving.farthestMade(0, 1, 10, value -> value < 0.5 ? Optional.of("at " + value) : Optional.empty());
        assertEquals("at 0.499", made.orElseThrow().substring(0, 8));
        assertTrue(Halving.farthestMade(0, 1, 10, value -> Optional.empty()).isEmpty());
    }

    @Test
    void comesBackTowardTheStartUntilTheTestHolds() {
        assertEquals(0.25, Halving.firstHeldShare(10, share -> share <= 0.3).orElseThrow());
        assertEquals(1, Halving.firstHeldShare(10, share -> true).orElseThrow());
        assertTrue(Halving.firstHeldShare(3, share -> share < 0.01).isEmpty());
    }
}
