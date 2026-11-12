package org.jmouse.ipc2581;

import java.util.List;

/**
 * A closed area: its outline, less its cut-outs — the board's profile, a pad no standard shape draws, a poured zone.
 * Each ring is its points in order, not closed by repeating the first: the file closes it.
 */
public record Ipc2581Polygon(List<Ipc2581Point> outline, List<List<Ipc2581Point>> cutouts) {

    public Ipc2581Polygon {
        if (outline.size() < 3) {
            throw new IllegalArgumentException("An area's outline has three points at least");
        }
        outline = List.copyOf(outline);
        cutouts = cutouts.stream().filter(cutout -> cutout.size() >= 3).map(List::copyOf).toList();
    }

    public static Ipc2581Polygon of(List<Ipc2581Point> outline) {
        return new Ipc2581Polygon(outline, List.of());
    }
}
