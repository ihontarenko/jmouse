package org.jmouse.cad.mesh;

import org.jmouse.settings.Bounds;
import org.jmouse.settings.Label;
import java.util.List;
import java.util.Map;

/**
 * What a model's markings are read by ({@code plugin/cad-mesh/markings}): the colour code a resistance is banded in, and where a
 * part's marks are found — what its body prints, how loose its value is. How a mark is painted is the interface's look.
 *
 * @param colourCode the colour code a resistance is written in as bands
 * @param part       where a part's marks are read from
 */
public record MarkingCodes(@Label("Colour code") ColourCode colourCode, @Label("Part") PartWords part) {

    /**
     * IEC 60062's colour code for resistors.
     *
     * @param source            where it is from
     * @param digits            the colour of each digit, 0 to 9
     * @param multipliers       the colour of each power of ten, by its exponent
     * @param tolerances        the colour of each tolerance, as a share of the value
     * @param usualTolerance    the tolerance a part that names none is banded with, as a share
     * @param digitsByTolerance how many digits a value is banded with: the first entry whose limit its tolerance is within
     */
    public record ColourCode(@Label("Source") String source, @Label("Digits") List<String> digits, @Label("Multipliers") Map<String, String> multipliers, @Label("Tolerances") List<Tolerance> tolerances,
                             @Bounds(minimum = 0, maximum = 1, step = 0.001) @Label("Usual tolerance") double usualTolerance, @Label("Digits by tolerance") List<DigitsByTolerance> digitsByTolerance) {

        public ColourCode {
            digits = List.copyOf(digits);
            multipliers = Map.copyOf(multipliers);
            tolerances = List.copyOf(tolerances);
            digitsByTolerance = List.copyOf(digitsByTolerance);
        }
    }

    /**
     * A tolerance's band.
     *
     * @param share  the tolerance, as a share of the value
     * @param colour its colour's word
     */
    public record Tolerance(@Label("Share") double share, @Label("Colour") String colour) {
    }

    /**
     * How many digits a value is banded with, up to a tolerance.
     *
     * @param atMost the loosest tolerance this holds for, as a share
     * @param digits the digits
     */
    public record DigitsByTolerance(@Label("At most") double atMost, @Label("Digits") int digits) {
    }

    /**
     * Where a part's marks are read from.
     *
     * @param bodyText           the portrait text a component prints on its body: {@code marking}
     * @param toleranceParameter the parameter a part's tolerance is: {@code tolerance}
     */
    public record PartWords(@Label("Body text") String bodyText, @Label("Tolerance parameter") String toleranceParameter) {
    }
}
