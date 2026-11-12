package org.jmouse.cad.pcb.layout;

import org.jmouse.cad.settings.SettingsOwner;
import org.jmouse.cad.settings.SettingsSection;
import java.util.List;

/**
 * How a pcb is laid out, as data ({@link #SECTION}): a new pcb from its circuit, the parts added to one later, what a part with
 * no footprint takes, the marks a placement machine finds a pcb by, the letters the pcb's own parts are named with, how near
 * the ends of an edge being drawn join, and the copper stacks a pcb is made with. One home: the engine lays out with it, and
 * the editor is served it ({@code GET /settings/pcb}) for what it draws and sets out while a hand works.
 *
 * @param circuit                 a new pcb laid out from its circuit's sheet
 * @param unseated                what a part with no footprint read takes on the pcb
 * @param setOut                  how parts added to a pcb after it was laid out are set out beside it
 * @param fiducials               the marks a placement machine finds a pcb by
 * @param ownPlacementDesignator  the letters a part only the pcb has is named with, before its number
 * @param edgeJoinReach           how near, in millimetres, the end of an edge being drawn joins an open end already there
 * @param copperStacks            how many copper layers a pcb may be made with
 */
public record PcbLayout(CircuitLayout.Settings circuit, Unseated unseated, SetOut setOut, Fiducials fiducials, String ownPlacementDesignator,
                        double edgeJoinReach, List<Integer> copperStacks) {

    /** Its section of the settings tree (ADR 0004): its file and schema under org/jmouse/cad/settings/pcb/layout. */
    public static final SettingsSection<PcbLayout> SECTION = new SettingsSection<>("pcb/layout", "PCB layout", SettingsOwner.ENGINE_DATA, PcbLayout.class);

    public PcbLayout {
        copperStacks = List.copyOf(copperStacks);
    }

    /**
     * A part with no footprint read: a box this large, its courtyard this far round it, its designator this tall, in millimetres.
     *
     * @param width            the box's width, in millimetres
     * @param height           the box's height, in millimetres
     * @param courtyard        how far its courtyard stands round the box, in millimetres
     * @param designatorHeight how tall its designator is written over it, in millimetres
     */
    public record Unseated(double width, double height, double courtyard, double designatorHeight) {
    }

    /**
     * Parts set out in rows, each row no wider than {@code rowWidth}, {@code margin} apart and from the edge; a pcb drawn round them
     * no smaller than {@code least}, in millimetres.
     *
     * @param rowWidth how wide a row of parts runs before the next row starts, in millimetres
     * @param margin   how far apart parts stand, and from the edge drawn round them, in millimetres
     * @param least    the least size the pcb drawn round them is
     */
    public record SetOut(double rowWidth, double margin, Least least) {
    }

    /**
     * The least size a pcb is drawn at, in millimetres.
     *
     * @param width  the least width, in millimetres: a pcb one could order however few its parts
     * @param height the least height, in millimetres
     */
    public record Least(double width, double height) {
    }

    /**
     * The footprint a fiducial stands on, the letters it is named with, and how far in from the edge's corners, in millimetres.
     *
     * @param footprint  the footprint a fiducial stands on, by slug
     * @param designator the letters a fiducial is named with, before its number
     * @param inset      how far in from the corners of the edge's box a fiducial stands, in millimetres
     */
    public record Fiducials(String footprint, String designator, double inset) {
    }
}
