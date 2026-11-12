package org.jmouse.cad.plugin.drawings;

import org.jmouse.dxf.DxfOutline;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * A board's edge from a DXF a mechanical program drew: the widest closed outline its edge, the others inside it its cut-outs, as
 * one path the pcb's outline is written in, standing at the sheet's origin with Y down as a pcb's is.
 */
final class OutlineFromDxf {

    /** The outline, and what of the file it could not use. */
    record Answer(String path, List<String> notes) {
    }

    private OutlineFromDxf() {
    }

    private static double area(List<DxfOutline.Point> ring) {
        double twice = 0;
        for (int index = 0; index < ring.size(); index++) {
            DxfOutline.Point from = ring.get(index);
            DxfOutline.Point to = ring.get((index + 1) % ring.size());
            twice += from.x() * to.y() - to.x() * from.y();
        }
        return Math.abs(twice) / 2;
    }

    static Answer of(String text) {
        DxfOutline.Read read = DxfOutline.read(text);
        if (read.closed().isEmpty()) {
            throw new IllegalArgumentException("The DXF holds no closed outline" + (read.open() > 0 ? ": " + read.open() + " run(s) of lines never come back to where they begin." : "."));
        }
        List<List<DxfOutline.Point>> rings = read.closed().stream().sorted(Comparator.comparingDouble((List<DxfOutline.Point> ring) -> area(ring)).reversed()).toList();
        List<DxfOutline.Point> edge = rings.getFirst();
        double left = edge.stream().mapToDouble(DxfOutline.Point::x).min().orElse(0);
        double top = edge.stream().mapToDouble(DxfOutline.Point::y).max().orElse(0);
        StringBuilder path = new StringBuilder();
        for (List<DxfOutline.Point> ring : rings) {
            for (int index = 0; index < ring.size(); index++) {
                DxfOutline.Point point = ring.get(index);
                path.append(index == 0 ? "M" : "L").append(String.format(Locale.ROOT, "%.3f %.3f", point.x() - left, top - point.y()));
            }
            path.append("Z ");
        }
        List<String> notes = new ArrayList<>();
        if (read.unitless()) {
            notes.add("The DXF says no unit: it was read in millimetres.");
        }
        if (read.open() > 0) {
            notes.add(read.open() + " run(s) of lines did not close and were left out.");
        }
        if (read.skipped() > 0) {
            notes.add(read.skipped() + " other thing(s) of the drawing — text, dimensions — were left out.");
        }
        return new Answer(path.toString().trim(), notes);
    }
}
