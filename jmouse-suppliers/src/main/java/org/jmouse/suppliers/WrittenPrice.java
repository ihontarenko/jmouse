package org.jmouse.suppliers;

import java.math.BigDecimal;

/**
 * A price as a distributor writes it for an account's locale — {@code $0.176}, {@code 0,176 €}, {@code 1.234,50 €}.
 *
 * <p>⚠️ <strong>The separator has to be worked out, not stripped.</strong> Keeping only digits and dots turns
 * {@code 0,176 €} into 176: a thousandfold price that looks entirely plausible on a screen. Where both a comma and a dot
 * appear, the last is the decimal separator; where one kind appears, once is a decimal separator and repeatedly is
 * grouping. A unit price is never large enough for a lone {@code 1,234} to be a thousand.</p>
 */
final class WrittenPrice {

    private WrittenPrice() {
    }

    /** The price; null — never zero, which reads as free — where the text holds no number. */
    static BigDecimal parse(String written) {
        if (written == null) {
            return null;
        }
        StringBuilder kept = new StringBuilder();
        written.chars().filter(character -> Character.isDigit(character) || character == ',' || character == '.')
                .forEach(kept::appendCodePoint);
        String number = kept.toString();
        int lastComma = number.lastIndexOf(',');
        int lastDot = number.lastIndexOf('.');
        if (lastComma >= 0 && lastDot >= 0) {
            char decimal = lastComma > lastDot ? ',' : '.';
            char grouping = decimal == ',' ? '.' : ',';
            number = number.replace(String.valueOf(grouping), "").replace(decimal, '.');
        } else {
            number = withOneSeparator(number, lastComma >= 0 ? ',' : '.');
        }
        try {
            return new BigDecimal(number);
        } catch (NumberFormatException noNumber) {
            return null;
        }
    }

    /** Once it is the decimal separator; repeated, it is grouping and goes. */
    private static String withOneSeparator(String number, char separator) {
        if (number.indexOf(separator) == number.lastIndexOf(separator)) {
            return number.replace(separator, '.');
        }
        return number.replace(String.valueOf(separator), "");
    }
}
