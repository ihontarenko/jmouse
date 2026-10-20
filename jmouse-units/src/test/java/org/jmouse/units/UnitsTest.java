package org.jmouse.units;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UnitsTest {

    @Test
    void readsValuesAsADatasheetWritesThem() {
        assertQuantity("4.7 nF", 4.7e-9, "F");
        assertQuantity("0.3 V/us", 3e5, "V/s");
        assertQuantity("0.3 V/µs", 3e5, "V/s");
        assertQuantity("10 kΩ", 1e4, "Ohm");
        assertQuantity("52.4%", 0.524, "%");
        assertQuantity("90 deg", Math.PI / 2, "rad");
        assertQuantity("100 ppm/K", 1e-4, "ppm/K");
        assertQuantity("2 A/V^2", 2, "A/V^2");
        assertQuantity("1 kg", 1e3, "g");
        assertQuantity("25", 25, "");
    }

    @Test
    void keepsTheUnitAsWritten() {
        assertEquals("-40 °C", Quantity.parse("-40 °C").orElseThrow().written());
        assertEquals("2.2 µs", Quantity.parse("2.2µs").orElseThrow().written());
        assertEquals("52.4%", Quantity.parse("52.4 %").orElseThrow().written());
    }

    @Test
    void refusesWhatIsNotAValue() {
        assertTrue(Quantity.parse("2pi").isEmpty());
        assertTrue(Quantity.parse("1 / (1 / f)").isEmpty());
        assertTrue(Quantity.parse("5 k%").isEmpty(), "a prefix does not stand before a unit of its own");
        assertTrue(Quantity.parse("74hc595").isEmpty());
        assertFalse(Quantity.isUnit("kHz/"));
        assertTrue(Quantity.isUnit("kOhm"));
    }

    @Test
    void measuresDimensions() {
        assertEquals(Optional.of(Dimension.VOLTAGE), Dimension.ofUnit("mV"));
        assertEquals(Optional.of(Dimension.RESISTANCE), Dimension.ofUnit("kΩ"));
        assertEquals(Optional.of(Dimension.FREQUENCY), Dimension.ofUnit("1/s"));
        assertEquals(Optional.of(Dimension.TEMPERATURE.over(Dimension.POWER)), Dimension.ofUnit("K/W"));
        assertEquals(Optional.of(Dimension.CURRENT.over(Dimension.VOLTAGE.power(2))), Dimension.ofUnit("A/V^2"));
        assertEquals(Optional.of(Dimension.LENGTH.power(2)), Dimension.ofUnit("m²"));
        assertEquals(Optional.of(Dimension.TEMPERATURE), Dimension.ofUnit("°C"));
        assertEquals(Optional.of(Dimension.NONE), Dimension.ofUnit("%"));
        assertTrue(Dimension.ofUnit("div").isEmpty());
        assertTrue(Dimension.ofUnit("bit").isEmpty());
        assertEquals(Optional.of(Dimension.CAPACITANCE), Quantity.parse("4.7 nF").orElseThrow().dimension());
    }

    @Test
    void showsAndReadsSymbols() {
        assertEquals("µF", Units.shown("uF"));
        assertEquals("kΩ", Units.shown("kOhm"));
        assertEquals("degC", Units.asRead("°C"));
        assertEquals(Optional.of(SiPrefix.MICRO), SiPrefix.of('µ'));
        assertEquals(Optional.of(SiPrefix.MEGA), SiPrefix.of('M'));
        assertEquals(Optional.of(SiPrefix.MILLI), SiPrefix.of('m'));
    }

    private static void assertQuantity(String written, double value, String unit) {
        Quantity quantity = Quantity.parse(written).orElseThrow(() -> new AssertionError(written + " did not read"));
        assertEquals(value, quantity.value(), Math.abs(value) * 1e-12, written);
        assertEquals(unit, quantity.unit(), written);
    }
}
