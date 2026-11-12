package org.jmouse.cad.libre.pcb;

import org.jmouse.cad.exchange.imported.RoundedPath;
import org.jmouse.libre.pcb.LibrePcbGraphic;
import org.jmouse.libre.pcb.LibrePcbPoint;

import java.util.List;
import java.util.function.Function;

/** LibrePCB's corners as a path, each side straight or bent as its corner says, in whatever frame {@code place} takes them to. */
final class LibrePcbPaths {

    private LibrePcbPaths() {
    }

    static void corners(List<LibrePcbGraphic.Vertex> vertices, RoundedPath path, Function<LibrePcbPoint, double[]> place) {
        if (vertices.isEmpty()) {
            return;
        }
        double[] first = place.apply(vertices.getFirst().at());
        path.moveTo(first[0], first[1]);
        for (int index = 0; index + 1 < vertices.size(); index++) {
            LibrePcbGraphic.Vertex from = vertices.get(index);
            LibrePcbGraphic.Vertex to = vertices.get(index + 1);
            double[] end = place.apply(to.at());
            if (from.bends()) {
                double[] start = place.apply(from.at());
                double[] middle = place.apply(from.middleTowards(to));
                path.arcThrough(start[0], start[1], middle[0], middle[1], end[0], end[1]);
            } else {
                path.lineTo(end[0], end[1]);
            }
        }
        if (vertices.size() > 2 && vertices.getFirst().at().equals(vertices.getLast().at())) {
            path.close();
        }
    }
}
