package org.jmouse.kicad.connectivity;

import org.jmouse.kicad.symbol.KiCadPin;

import java.math.BigInteger;
import java.util.Comparator;
import java.util.List;

/**
 * The name KiCad gives a net nothing names — no power symbol, no label —, after one of its pins:
 *
 * <ul>
 *     <li>a pin with a name is taken before one without: {@code Net-(U2-+)}, not {@code Net-(R11-Pad2)};</li>
 *     <li>among those, the lowest reference in natural order — {@code R9} before {@code R12} —, then the lowest pin;</li>
 *     <li>a symbol of several units is named with its unit's letter: {@code U5A};</li>
 *     <li>{@code Net-(REF-NAME)} for a named pin, {@code Net-(REF-PadNUMBER)} otherwise; a net of one pin is
 *     {@code unconnected-(REF-NAME-PadNUMBER)};</li>
 *     <li>a {@code /} in a pin's name is written {@code {slash}}, as a net's name may not hold one.</li>
 * </ul>
 *
 * <p>Read off KiCad 8's own netlists of two real designs: one names 43 nets of 43 alike; the other 110 of 118. Where it
 * differs, KiCad took an unnamed pin over a named one of the same type — {@code R29-Pad2} over {@code SW1-B} —, by a
 * rule not yet known. The pins of every net match; a generated name is never written to a schematic file.</p>
 */
final class KiCadNetNames {

    /** Natural order: the digits in a reference compared as numbers. */
    static final Comparator<String> NATURAL = KiCadNetNames::compareNaturally;

    static final Comparator<KiCadConnection> PIN_ORDER = Comparator.comparing(KiCadConnection::shownReference, NATURAL)
            .thenComparing(connection -> connection.pin().number(), NATURAL);

    private KiCadNetNames() {
    }

    /** The name of a net its pins alone name. */
    static String nameOf(List<KiCadConnection> pins) {
        KiCadConnection chosen = pins.stream().filter(KiCadNetNames::named).min(PIN_ORDER)
                .orElseGet(() -> pins.stream().min(PIN_ORDER).orElseThrow());
        String pin = escaped(chosen.pin().name());
        if (pins.size() == 1) {
            return "unconnected-(" + chosen.shownReference() + (named(chosen) ? "-" + pin : "") + "-Pad" + chosen.pin().number() + ")";
        }
        return "Net-(" + chosen.shownReference() + "-" + (named(chosen) ? pin : "Pad" + chosen.pin().number()) + ")";
    }

    private static boolean named(KiCadConnection connection) {
        String name = connection.pin().name();
        return !name.isEmpty() && !name.equals(KiCadPin.NO_NAME);
    }

    private static String escaped(String name) {
        return name.replace("/", "{slash}");
    }

    private static int compareNaturally(String one, String other) {
        int first = 0;
        int second = 0;
        while (first < one.length() && second < other.length()) {
            char left = one.charAt(first);
            char right = other.charAt(second);
            if (Character.isDigit(left) && Character.isDigit(right)) {
                int leftEnd = digitsEnd(one, first);
                int rightEnd = digitsEnd(other, second);
                int compared = new BigInteger(one.substring(first, leftEnd)).compareTo(new BigInteger(other.substring(second, rightEnd)));
                if (compared != 0) {
                    return compared;
                }
                first = leftEnd;
                second = rightEnd;
            } else {
                if (left != right) {
                    return Character.compare(left, right);
                }
                first++;
                second++;
            }
        }
        return Integer.compare(one.length() - first, other.length() - second);
    }

    private static int digitsEnd(String text, int from) {
        int end = from;
        while (end < text.length() && Character.isDigit(text.charAt(end))) {
            end++;
        }
        return end;
    }
}
