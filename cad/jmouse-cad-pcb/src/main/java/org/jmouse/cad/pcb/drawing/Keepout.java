package org.jmouse.cad.pcb.drawing;

import java.util.List;

/**
 * An area where no copper is laid.
 *
 * @param layers the layers it keeps copper off
 * @param path   the area, as path data
 */
public record Keepout(List<String> layers, String path) {

    public Keepout {
        layers = List.copyOf(layers);
    }
}
