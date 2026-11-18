package org.jmouse.cad.pcb.drawing;

import org.jmouse.cad.pcb.PcbVocabulary;

/**
 * A copper pour: an area of a layer filled with one net's copper, round whatever else stands in it.
 *
 * @param layer     the copper layer it is poured on
 * @param net       the net its copper carries
 * @param clearance how far it stands from other nets' copper, millimetres; 0 for the pcb's own
 * @param priority  which pour is filled first where two claim an area: the higher; 0 for the lowest
 * @param path      the area it fills, as path data
 * @param finish    what its copper is left as: bare, the solder mask open over all of it; empty for under the mask
 */
public record Zone(String layer, String net, double clearance, int priority, String path, String finish) {

    public Zone {
        finish = finish == null ? "" : finish;
    }

    /** A pour under the mask. */
    public Zone(String layer, String net, double clearance, int priority, String path) {
        this(layer, net, clearance, priority, path, "");
    }

    public boolean bare() {
        return PcbVocabulary.FINISH_BARE.equals(finish);
    }
}
