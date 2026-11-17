package org.jmouse.settings;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecordChecksTest {

    record Layer(@Colour String ink, @Bounds(minimum = 0, maximum = 1, step = 0.01) double opacity) {
    }

    record Look(@Bounds(minimum = 1, maximum = 9, step = 1) int digits, List<Layer> layers, @Colour Optional<String> paper,
                @Colour List<String> inner, Map<String, Layer> named) {
    }

    private static Look look(double opacity, String ink, int digits) {
        return new Look(digits, List.of(new Layer("#112233", 0.5), new Layer(ink, opacity)), Optional.of("#ffffff"), List.of("#000000ff"),
                        Map.of("top", new Layer("#abcdef", 1)));
    }

    @Test
    void passesWhatItsMarksAllow() {
        assertTrue(RecordChecks.refusalOf(look(0.35, "#445566", 4)).isEmpty());
    }

    @Test
    void refusesANumberOutsideItsBoundsNamingWhereItStands() {
        assertEquals(Optional.of("layers[1].opacity runs from 0 to 1: 1.5 is outside."), RecordChecks.refusalOf(look(1.5, "#445566", 4)));
    }

    @Test
    void refusesANumberOffItsStep() {
        assertEquals(Optional.of("layers[1].opacity moves in steps of 0.01: 0.355 is off them."), RecordChecks.refusalOf(look(0.355, "#445566", 4)));
    }

    @Test
    void takesADecimalWrittenInTextAsOnItsStep() {
        assertTrue(RecordChecks.refusalOf(look(0.1 + 0.2, "#445566", 4)).isEmpty());
    }

    @Test
    void refusesAWordThatIsNoColour() {
        assertEquals(Optional.of("layers[1].ink is a colour, written #rrggbb or #rrggbbaa: \"red\" is not one."),
                     RecordChecks.refusalOf(look(0.5, "red", 4)));
    }

    @Test
    void holdsAWholeNumberToItsBounds() {
        assertEquals(Optional.of("digits runs from 1 to 9: 12 is outside."), RecordChecks.refusalOf(look(0.5, "#445566", 12)));
    }
}
