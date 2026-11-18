package org.jmouse.cad.pcb.drawing;

import java.util.List;

/**
 * A class of nets, routed alike.
 *
 * @param name      what it is called
 * @param width     the width its tracks are laid at, millimetres
 * @param clearance how far its copper stands from other nets', millimetres
 * @param drill     its vias' drill, millimetres; 0 with {@code diameter}: the pcb's usual via
 * @param diameter  its vias' copper ring across, millimetres; 0 for the pcb's usual via
 * @param nets      the nets it holds
 */
public record NetClass(String name, double width, double clearance, double drill, double diameter, List<String> nets) {

    public NetClass {
        nets = List.copyOf(nets);
    }
}
