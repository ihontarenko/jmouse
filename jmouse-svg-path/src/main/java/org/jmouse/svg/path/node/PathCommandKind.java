package org.jmouse.svg.path.node;

import java.util.Arrays;
import java.util.Optional;

/**
 * The commands of SVG path data, each its letter and how many numbers one of it takes — the whole grammar of a command,
 * as data. A letter in capitals is absolute, in lower case relative to where the pen stands.
 */
public enum PathCommandKind {

    MOVE('M', 2),
    LINE('L', 2),
    HORIZONTAL('H', 1),
    VERTICAL('V', 1),
    CUBIC('C', 6),
    SMOOTH_CUBIC('S', 4),
    QUADRATIC('Q', 4),
    SMOOTH_QUADRATIC('T', 2),
    /** rx ry rotation large-arc sweep x y */
    ARC('A', 7),
    CLOSE('Z', 0);

    private final char letter;
    private final int  arity;

    PathCommandKind(char letter, int arity) {
        this.letter = letter;
        this.arity = arity;
    }

    /** The absolute letter: {@code M}, {@code A}. */
    public char letter() {
        return letter;
    }

    /** How many numbers one command of it takes. */
    public int arity() {
        return arity;
    }

    /** The letter it is written with, absolute or relative. */
    public char letter(boolean relative) {
        return relative ? Character.toLowerCase(letter) : letter;
    }

    /**
     * What more numbers after a command of it are: more of the same, except after a move, where they are lines — SVG's
     * {@code M 0 0 10 10} draws to (10, 10).
     */
    public PathCommandKind repeatedAs() {
        return this == MOVE ? LINE : this;
    }

    public static Optional<PathCommandKind> of(char letter) {
        char absolute = Character.toUpperCase(letter);
        return Arrays.stream(values()).filter(kind -> kind.letter == absolute).findFirst();
    }

    /** Whether a character is a command's letter, in either case. */
    public static boolean isLetter(char character) {
        return of(character).isPresent();
    }
}
