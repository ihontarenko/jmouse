package org.jmouse.cad.pcb.drawing;

import java.util.List;

/**
 * The sizes a pcb offers new copper in, beside what its classes say: track widths, and via drills each with its
 * diameter. Empty lists: the editor's own.
 *
 * @param tracks the track widths offered, millimetres
 * @param vias   the vias offered, each a drill with its copper ring
 */
public record Sizes(List<Double> tracks, List<ViaSize> vias) {

    public static final Sizes NONE = new Sizes(List.of(), List.of());

    public Sizes {
        tracks = List.copyOf(tracks);
        vias = List.copyOf(vias);
    }
}
