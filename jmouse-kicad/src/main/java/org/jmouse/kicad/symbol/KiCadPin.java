package org.jmouse.kicad.symbol;

import static org.jmouse.kicad.KiCadKeyword.*;

import org.jmouse.kicad.KiCadPoint;
import org.jmouse.kicad.parser.node.KiCadList;

/**
 * A pin of a library symbol: {@code (pin passive line (at 0 3.81 270) (length 1.27) (name "~") (number "1"))}.
 *
 * @param number     what the package calls it: {@code 1}, {@code A3}
 * @param name       what it does — {@code ~} for no name, as KiCad writes it
 * @param type       its electrical type: {@code passive}, {@code input}, {@code power_in}, {@code no_connect} …
 * @param shape      how it is drawn: {@code line}, {@code inverted} (a bubble at the body), {@code clock} …
 * @param at         its connection point, in the symbol's own drawing — Y upwards: where a wire meets it
 * @param angle      which way it points from that point into the body, degrees
 * @param length     how long it is, millimetres
 * @param unit       the unit it belongs to; 0 for every unit
 * @param hidden     whether it is drawn: a hidden power pin still connects, by its name
 */
public record KiCadPin(String number, String name, KiCadPinType type, String shape, KiCadPoint at, int angle,
                       double length, int unit, boolean hidden) {

    /** What KiCad writes for a pin without a name. */
    public static final String NO_NAME = "~";

    /** A pin as a symbol's unit writes it. */
    static KiCadPin of(KiCadList pin, int unit) {
        KiCadList at = pin.requireList(AT);
        return new KiCadPin(pin.wordOf(NUMBER).orElse(""), pin.wordOf(NAME).orElse(NO_NAME), KiCadPinType.of(pin.word(1)),
                pin.word(2), KiCadPoint.of(at), at.size() > 3 ? (int) at.number(3) : 0,
                pin.list(LENGTH).map(length -> length.number(1)).orElse(0.0), unit, pin.says(HIDE));
    }

    /** Whether it is a power input KiCad joins to the net of its name even when nothing is wired to it. */
    public boolean joinsByName() {
        return hidden && type == KiCadPinType.POWER_INPUT;
    }

    /** What the pin is called on a net: its name, else its number. */
    public String shownName() {
        return name.equals(NO_NAME) || name.isEmpty() ? number : name;
    }
}
