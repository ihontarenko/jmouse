package org.jmouse.kicad.schematic;

import static org.jmouse.kicad.KiCadKeyword.*;

import org.jmouse.kicad.KiCadPoint;
import org.jmouse.kicad.parser.node.KiCadList;

import java.util.List;

/** A wire on a sheet: {@code (wire (pts (xy 77.47 44.45) (xy 77.47 62.23)) …)} — a straight segment between its points. */
public record KiCadWire(List<KiCadPoint> points) {

    public KiCadWire {
        points = List.copyOf(points);
    }

    static KiCadWire of(KiCadList wire) {
        return new KiCadWire(wire.requireList(POINTS).lists(POINT).stream().map(KiCadPoint::of).toList());
    }

    public KiCadPoint start() {
        return points.getFirst();
    }

    public KiCadPoint end() {
        return points.getLast();
    }
}
