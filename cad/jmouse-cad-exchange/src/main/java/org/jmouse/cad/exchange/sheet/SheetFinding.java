package org.jmouse.cad.exchange.sheet;

import org.jmouse.cad.Point;
import org.jmouse.cad.document.FindingSeverity;

import java.util.List;
import java.util.Optional;

/**
 * What a sheet's checks find: how much it weighs, what it says, what it is about — the keys an editor selects it by,
 * {@code item:R1} —, and where it is on the sheet, to be marked.
 */
public record SheetFinding(FindingSeverity severity, String message, List<String> keys, Optional<Point> at) {

    public SheetFinding {
        keys = List.copyOf(keys);
    }
}
