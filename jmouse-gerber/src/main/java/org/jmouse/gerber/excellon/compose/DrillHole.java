package org.jmouse.gerber.excellon.compose;

import org.jmouse.gerber.GerberPoint;

/** What a hole is: drilled round at a point, or a slot routed between two points, its width the tool's. */
public sealed interface DrillHole {

    record Round(GerberPoint at) implements DrillHole {
    }

    record Slot(GerberPoint from, GerberPoint to) implements DrillHole {
    }
}
