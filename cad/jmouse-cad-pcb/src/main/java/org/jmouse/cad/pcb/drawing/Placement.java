package org.jmouse.cad.pcb.drawing;

import org.jmouse.cad.Point;
import org.jmouse.cad.pcb.PcbVocabulary;
import org.jmouse.cad.drawing.Arrangement;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Where a part stands on the pcb.
 *
 * @param designator  the part it places, as the circuit names it
 * @param x           where its origin stands, millimetres from the left
 * @param y           where its origin stands, millimetres down
 * @param rotation    how far it is turned, degrees counter-clockwise as the pcb is seen
 * @param side        {@code top} or {@code bottom}
 * @param reference   where its designator stands in the part's own frame; empty for where its footprint's field puts it
 * @param shown       what is printed for it where that is not its designator — {@code R1} for {@code S1_R1} —; empty for its designator
 * @param footprint   the footprint a part that is only the pcb's stands on — a mounting hole, a logo —; empty for a part of the circuit
 * @param nets        the pad a net stands on, by net — {@code nets (GND = 1)} —, for a part that is only the pcb's
 * @param arrangement the group it is chosen and moved with, and whether it is locked where it stands
 * @param role        what it is to the copper beyond its pads: {@code link} for a 0 Ω link, its pads one piece of copper; empty for a part
 */
public record Placement(String designator, double x, double y, double rotation, String side, Optional<Reference> reference,
                        Optional<String> shown, Optional<String> footprint, Map<String, String> nets, Arrangement arrangement, String role) {

    public Placement {
        nets = nets == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(nets));
        arrangement = arrangement == null ? Arrangement.FREE : arrangement;
        role = role == null ? "" : role;
    }

    /** A placement in no group, free. */
    public Placement(String designator, double x, double y, double rotation, String side, Optional<Reference> reference,
                     Optional<String> shown, Optional<String> footprint, Map<String, String> nets) {
        this(designator, x, y, rotation, side, reference, shown, footprint, nets, Arrangement.FREE, "");
    }

    /** The same part standing otherwise among the others: in another group, or locked. */
    public Placement arranged(Arrangement other) {
        return new Placement(designator, x, y, rotation, side, reference, shown, footprint, nets, other, role);
    }

    /** A part of the circuit, printed as it is named and standing on the footprint the circuit gives it. */
    public Placement(String designator, double x, double y, double rotation, String side, Optional<Reference> reference) {
        this(designator, x, y, rotation, side, reference, Optional.empty(), Optional.empty(), Map.of());
    }

    /** What is printed on the pcb and listed for a person: on an unfolded design a part is {@code S1_R1} but reads {@code R1}. */
    public String printed() {
        return shown.orElse(designator);
    }

    /** Whether it is only the pcb's — a mounting hole, a logo — and no part of the circuit: it names its own footprint. */
    public boolean ownedByPcb() {
        return footprint.isPresent();
    }

    public boolean onBottom() {
        return PcbVocabulary.BOTTOM.equals(side);
    }

    /**
     * Where a point of the part's footprint lands on the pcb: mirrored first when the part sits on the bottom, then
     * turned with the part, counter-clockwise as the pcb is seen.
     */
    public Point placed(double footprintX, double footprintY) {
        double mirroredX = onBottom() ? -footprintX : footprintX;
        double radians = Math.toRadians(-rotation);
        double cosine = Math.cos(radians);
        double sine = Math.sin(radians);
        return new Point(x + mirroredX * cosine - footprintY * sine, y + mirroredX * sine + footprintY * cosine);
    }

    /** The point of the part's footprint that lands on a point of the pcb: {@link #placed} undone. */
    public Point footprintPointAt(Point onPcb) {
        double radians = Math.toRadians(-rotation);
        double cosine = Math.cos(radians);
        double sine = Math.sin(radians);
        double dx = onPcb.x() - x;
        double dy = onPcb.y() - y;
        double mirroredX = dx * cosine + dy * sine;
        double footprintY = -dx * sine + dy * cosine;
        return new Point(onBottom() ? -mirroredX : mirroredX, footprintY);
    }

    /** Where a point of the pcb that stood on this part goes when the part stands as {@code moved} does: the same spot of it. */
    public Point carriedTo(Placement moved, Point onPcb) {
        Point local = footprintPointAt(onPcb);
        return moved.placed(local.x(), local.y());
    }
}
