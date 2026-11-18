package org.jmouse.cad.pcb.drawing;

/**
 * Something drawn on a layer of the pcb itself, which no part brought: a logo on the silkscreen, a window in the mask, a
 * note on the documentation, copper that is no track.
 *
 * @param layer  the layer's id: {@code top}, {@code inner-1}, {@code top-silkscreen}, {@code bottom-mask}, {@code comments}
 * @param width  what it is stroked with; 0 when it is filled
 * @param filled whether it is an area rather than lines
 * @param net    the net copper carries; empty for none
 * @param path   what it draws, as path data
 */
public record Shape(String layer, double width, boolean filled, String net, String path) {

    public Shape {
        net = net == null ? "" : net;
    }
}
