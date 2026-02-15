package org.jmouse.util;

import java.math.BigDecimal;

public final class Numbers {

    private Numbers() {}

    public static BigDecimal toDecimal(Object value) {
        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal;
        }

        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }

        try {
            return new BigDecimal(value.toString());
        } catch (Exception e) {
            return null;
        }
    }

}
