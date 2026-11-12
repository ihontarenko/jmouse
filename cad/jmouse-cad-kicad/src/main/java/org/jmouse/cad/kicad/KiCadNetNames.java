package org.jmouse.cad.kicad;

/**
 * What KiCad writes into a net's name that is no part of the net: the path of the sheet it is on — {@code /control/IN}
 * is {@code IN} — and the name it makes up for an unnamed one — {@code Net-(R5-Pad1)} is {@code net_R5_Pad1}. The rest of
 * naming a net is every import's ({@link org.jmouse.cad.exchange.imported.ImportedNetNames}).
 */
final class KiCadNetNames {

    /** What KiCad's ground symbol names its net. */
    static final String KICAD_GROUND = "GND";
    /** What a net of one pad nothing else joins opens with: it is on no net of the schematic. */
    static final String UNCONNECTED = "unconnected-";

    private KiCadNetNames() {
    }

    /** A KiCad net's name without its sheet's path, an unnamed one's made-up name as a word. */
    static String local(String kicad) {
        String name = kicad.replaceAll(".*/", "");
        return name.startsWith("Net-(") ? "net_" + name.substring(5).replace(")", "") : name;
    }
}
