package org.jmouse.suppliers;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class WrittenPriceTest {

    @Test
    void readsEveryLocaleItIsWrittenIn() {
        assertEquals(new BigDecimal("0.176"), WrittenPrice.parse("$0.176"));
        assertEquals(new BigDecimal("0.176"), WrittenPrice.parse("0,176 €"));
        assertEquals(new BigDecimal("1234.50"), WrittenPrice.parse("1.234,50 €"));
        assertEquals(new BigDecimal("1234.50"), WrittenPrice.parse("$1,234.50"));
        assertEquals(new BigDecimal("1234567"), WrittenPrice.parse("1,234,567"));
    }

    @Test
    void whatIsNotAPriceIsNullNeverZero() {
        assertNull(WrittenPrice.parse(null));
        assertNull(WrittenPrice.parse("See quote"));
        assertNull(WrittenPrice.parse(""));
        assertNull(WrittenPrice.parse("."));
    }
}
