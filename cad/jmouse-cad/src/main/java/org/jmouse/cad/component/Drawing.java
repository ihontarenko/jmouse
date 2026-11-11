package org.jmouse.cad.component;

import org.jmouse.cad.Given;

import java.util.List;
import java.util.Optional;

/**
 * How a component is drawn — its {@code symbol} — or how it looks — its {@code portrait} —: the drawings it uses, by
 * slug, or a box of its own, and what the drawing's inputs are bound to.
 *
 * @param uses     the drawings it may be drawn with, the first its default; empty for a box
 * @param box      the sides of the box it is drawn as, or empty for a drawing it uses
 * @param bindings what the drawing's inputs are, from the part's values and pins
 */
public record Drawing(List<String> uses, List<BoxSide> box, List<Binding> bindings) {

    public Drawing {
        uses = List.copyOf(uses);
        box = List.copyOf(box);
        bindings = List.copyOf(bindings);
    }

    public boolean boxed() {
        return uses.isEmpty();
    }

    /** A side of a box and the pins it draws there, each with its marks: {@code left clock edge, cs bubble}. */
    public record BoxSide(String side, List<BoxPin> pins) {

        public BoxSide {
            pins = List.copyOf(pins);
        }
    }

    /** A pin on a box's side, and its marks — {@code edge}, {@code bubble}. */
    public record BoxPin(String pin, List<String> marks) {

        public BoxPin {
            marks = List.copyOf(marks);
        }
    }

    /** {@code failed = failed}, {@code a anode}: an input of the drawing, given a value or one of the part's pins. */
    public record Binding(String input, Given value, String pin) {

        public Optional<Given> given() {
            return Optional.ofNullable(value);
        }

        public Optional<String> boundPin() {
            return Optional.ofNullable(pin);
        }
    }
}
