package org.jmouse.j2js.teavm.supplement.jdk;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Java's text of a double from JavaScript's: every double of {@code javascript-doubles.txt} — its bits, and its text as Node wrote
 * it — laid out as Java lays it, against {@link Double#toString(double)} of the same bits.
 */
class DoubleTextsTest {

    @Test
    void laysOutJavaScriptsShortestDigitsExactlyAsJavaWritesThem() throws IOException {
        List<String> differing = new ArrayList<>();
        int          read      = 0;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                DoubleTextsTest.class.getResourceAsStream("/javascript-doubles.txt"), StandardCharsets.UTF_8))) {
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                String[] parts  = line.split(" ");
                double   number = Double.longBitsToDouble(Long.parseUnsignedLong(parts[0], 16));
                String   laid   = DoubleTexts.javaText(parts[1]);
                read++;
                if (!Double.toString(number).equals(laid)) {
                    differing.add(parts[1] + " → " + laid + ", Java " + Double.toString(number));
                }
            }
        }
        assertTrue(read > 7000);
        assertEquals(List.of(), differing.subList(0, Math.min(differing.size(), 20)));
    }
}
