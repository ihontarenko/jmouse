package org.jmouse.cad.pcb;

import org.jmouse.settings.Label;
import org.jmouse.cad.settings.SettingsSection;

import java.util.Map;

/**
 * How a pcb's layers are named where room is short: the side in one letter and what the layer is in a whole
 * word, never half of one — {@code T.Copper}, {@code I1.Copper}, {@code B.Silkscreen}. A footprint is drawn as it is seen from
 * above, so its layers are the top's.
 *
 * @param places how a layer's place is written before its kind
 * @param kinds  what a layer on a side is made of or for, by its kind: {@code copper} is {@code Copper}
 * @param sheet  the layers that stand on neither side and that no factory makes, by their kind
 * @param edge   the pcb's edge, which stands on no side
 */
public record PcbLayerCodes(@Label("Places") Places places, @Label("Kinds") Map<String, String> kinds, @Label("Sheet") Map<String, String> sheet, @Label("Edge") String edge) {

    /** Its section of the settings tree (ADR 0004): {@code org/jmouse/cad/settings/pcb/layer-codes}. */
    public static final SettingsSection<PcbLayerCodes> SECTION = new SettingsSection<>("pcb/layer-codes", "Layer codes", PcbLayerCodes.class);

    public PcbLayerCodes {
        kinds = Map.copyOf(kinds);
        sheet = Map.copyOf(sheet);
    }

    /**
     * How a layer's place is written.
     *
     * @param top    the top of the stack: {@code T.}
     * @param bottom the bottom of the stack: {@code B.}
     * @param inner  an inner layer, {@code {number}} its number from the top: {@code I{number}.}
     */
    public record Places(@Label("Top") String top, @Label("Bottom") String bottom, @Label("Inner") String inner) {
    }
}
