package org.jmouse.libre.pcb;

import java.util.Optional;

/**
 * What a pad or a via does to the solder mask or the paste: {@code auto} as the board's rules say, {@code off} nothing,
 * or a clearance of its own.
 */
public sealed interface LibrePcbOpening {

    String AUTOMATIC = "auto";
    String OFF = "off";

    /** As the board's design rules say. */
    record Automatic() implements LibrePcbOpening {
    }

    /** Nothing: it stays covered, or takes no paste. */
    record Off() implements LibrePcbOpening {
    }

    /** Open, by this clearance. */
    record Clearance(double millimetres) implements LibrePcbOpening {
    }

    /** What a word says; a word this reader does not know is refused rather than taken for one of them. */
    static LibrePcbOpening of(Optional<String> word) {
        String written = word.orElse(AUTOMATIC);
        return switch (written) {
            case AUTOMATIC -> new Automatic();
            case OFF -> new Off();
            default -> LibrePcbValues.parsed(written).<LibrePcbOpening>map(Clearance::new).orElseThrow(
                    () -> new LibrePcbReadException("'" + written + "' is no way of opening a mask or a paste."));
        };
    }
}
