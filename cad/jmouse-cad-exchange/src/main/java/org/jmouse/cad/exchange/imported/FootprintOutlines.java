package org.jmouse.cad.exchange.imported;

import org.jmouse.cad.pcb.FootprintLines;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The lines a converted footprint draws, gathered as a footprint keeps them: one outline for each layer, width and fill,
 * its shapes in the order the product drew them, every number to a thousandth of a millimetre.
 */
public final class FootprintOutlines {

    /** A length to a thousandth of a millimetre: a micrometre is below what a pcb is made to. */
    public static final int PLACES = 3;

    private record Stroke(String layer, double width, boolean filled) {
    }

    private final Map<Stroke, RoundedPath> paths = new LinkedHashMap<>();

    /** The path the shapes of this layer, width and fill are drawn into. */
    public RoundedPath path(String layer, double width, boolean filled) {
        Stroke stroke = new Stroke(layer, new RoundedPath(PLACES).rounded(width), filled);
        return paths.computeIfAbsent(stroke, ignored -> new RoundedPath(PLACES));
    }

    /** Every outline something was drawn into. */
    public List<FootprintLines.Outline> outlines() {
        List<FootprintLines.Outline> outlines = new ArrayList<>();
        paths.forEach((stroke, path) -> {
            if (!path.isEmpty()) {
                outlines.add(new FootprintLines.Outline(stroke.layer(), stroke.width(), stroke.filled(), path.build()));
            }
        });
        return outlines;
    }
}
