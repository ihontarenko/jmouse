package org.jmouse.el.writer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** A double written as source: its shortest digits, plain — never an exponent, never a trailing zero —, a whole one as digits. */
class TextSourceWriterNumberTest {

    private static String written(double number) {
        TextSourceWriter writer = new TextSourceWriter(SourceLayout.GLUED);
        writer.number(number);
        return writer.text();
    }

    @ParameterizedTest
    @CsvSource({
            "57.145, 57.145", "-46.0375, -46.0375", "10.0, 10", "-0.0, 0", "0.0, 0", "0.1, 0.1", "1.0E-5, 0.00001",
            "2.5E-4, 0.00025", "1.0E7, 10000000", "1.2345678E7, 12345678", "123456789.5, 123456789.5",
            "9.007199254740993E15, 9007199254740992", "1.0E20, 100000000000000000000", "0.30000000000000004, 0.30000000000000004"
    })
    void writesADoubleInItsShortestPlainDigits(double number, String expected) {
        assertEquals(expected, written(number));
    }

    @Test
    void writesAHugeDoublePlain() {
        assertEquals(new BigDecimal("1.5E300").toPlainString(), written(1.5e300));
    }
}
