package org.jmouse.cad.pcb.physics;

import org.jmouse.settings.Label;
import org.jmouse.cad.settings.SettingsSection;
import java.util.Locale;
import java.util.Map;

/**
 * What a pcb's stack means for the signals and the heat on it, as constants: the dielectric's permittivity until a reader
 * says otherwise, how a neighbour lowers a track's impedance, light's speed, how a pcb gives off heat, and the widths
 * searched for an impedance. Data, a section of the settings tree; the copper's and the pcb's thickness are its way
 * of making, not this.
 *
 * @param source             where each value comes from: the papers, books and makers it is read from
 * @param permittivity       the dielectric's relative permittivity until a reader says otherwise
 * @param pair               how much a neighbour lowers a track's impedance, by how the track stands: 2·Z·(1 −
 *                           share·e^(−decay·gap/reach))
 * @param speedOfLight       millimetres per nanosecond
 * @param freeSpace          the impedance of free space, ohms
 * @param surfaceCoefficient watts a square metre of pcb gives off per kelvin
 * @param searchWidths       the widths searched for a track giving an impedance
 */
public record StackPhysics(@Label("Source") String source,
                           @Label("Permittivity") double permittivity,
                           @Label("Pair coupling") Map<String, Coupling> pair,
                           @Label("Speed of light") double speedOfLight,
                           @Label("Free space") double freeSpace,
                           @Label("Surface coefficient") double surfaceCoefficient,
                           @Label("Search widths") Search searchWidths) {

    /** Its section of the settings tree (ADR 0004): its file and schema under org/jmouse/cad/settings/pcb/physics/stack. */
    public static final SettingsSection<StackPhysics> SECTION = new SettingsSection<>("pcb/physics/stack", "Stack physics", StackPhysics.class);

    public StackPhysics {
        pair = Map.copyOf(pair);
        for (TrackLine line : TrackLine.values()) {
            if (!pair.containsKey(line.word())) {
                throw new IllegalArgumentException("The stack's physics say nothing of a pair laid as a " + line.word());
            }
        }
    }

    /**
     * How a neighbour lowers a track's impedance.
     *
     * @param share how much of a track's impedance a neighbour takes at no gap
     * @param decay how fast that falls as the gap grows, against the track's reach
     */
    public record Coupling(@Label("Share") double share,
                           @Label("Decay") double decay) {
    }

    /**
     * The widths searched for an impedance, millimetres, and how finely.
     *
     * @param least     the narrowest width searched
     * @param most      the widest
     * @param precision how finely
     */
    public record Search(@Label(value = "Narrowest width", unit = "mm") double least,
                         @Label(value = "Widest width", unit = "mm") double most,
                         @Label(value = "Precision", unit = "mm") double precision) {
    }

    /** How a track stands against its plane. */
    public enum TrackLine {
        /** On an outer layer, over a whole plane on the next one. */
        MICROSTRIP,
        /** On an inner layer, centred between planes. */
        STRIPLINE;

        public String word() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    /** How a neighbour lowers the impedance of a track standing so. */
    public Coupling couplingOf(TrackLine line) {
        return pair.get(line.word());
    }
}
