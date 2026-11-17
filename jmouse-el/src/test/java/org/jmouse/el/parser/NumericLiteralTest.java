package org.jmouse.el.parser;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A decimal literal is a double exactly when its text reads back as that double, else a BigDecimal: the short way — fifteen
 * significant digits, a normal finite double — and the long way answer alike.
 */
class NumericLiteralTest {

    @ParameterizedTest
    @ValueSource(strings = {"0.8", "57.145", "-46.0375", "0.0", "-0.0", "1e3", "2.5E-4", "123456789012345.0", "0.000000000000001",
            "1.500000000000000000000", "00012.5", "1e300", "38.1", "1e-320"})
    void readsADecimalThatReadsBackAsItsDouble(String written) {
        assertEquals(Double.parseDouble(written), LiteralParser.numericLiteral(written).getValue());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0.1234567890123456789", "1e400", "-1e400", "12345678901234567.5"})
    void keepsADecimalNoDoubleHoldsAsItsBigDecimal(String written) {
        assertEquals(new BigDecimal(written), LiteralParser.numericLiteral(written).getValue());
    }

    @Test
    void readsSixteenDigitsThatStillReadBackAsADouble() {
        assertEquals(0.1d + 0.2d, LiteralParser.numericLiteral("0.30000000000000004").getValue());
    }
}
