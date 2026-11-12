package org.jmouse.cad.pcb;

import java.util.Set;

/** The words a pcb's and a footprint's lines are written with, as data the grammars name. */
public final class PcbVocabulary {

    /** How many decimals a pcb's millimetres are written to: a ten-thousandth of a millimetre, finer than any pcb is made. */
    public static final int DECIMALS = 4;

    /** {@code circuit rc-discharging}: the circuit a pcb is laid out for. */
    public static final String CIRCUIT = "circuit";

    public static final String THROUGH_HOLE  = "through-hole";
    public static final String SURFACE       = "surface";
    public static final String CONNECTOR     = "connector";
    public static final String UNPLATED_HOLE = "unplated-hole";

    /** How a pad is made. */
    public static final Set<String> PAD_TYPES = Set.of(THROUGH_HOLE, SURFACE, CONNECTOR, UNPLATED_HOLE);

    public static final String CIRCLE            = "circle";
    public static final String RECTANGLE         = "rectangle";
    public static final String OVAL              = "oval";
    public static final String ROUNDED_RECTANGLE = "rounded-rectangle";
    public static final String CUSTOM            = "custom";

    /** The shape a pad is cut in. */
    public static final Set<String> PAD_SHAPES = Set.of(CIRCLE, RECTANGLE, OVAL, "trapezoid", ROUNDED_RECTANGLE, CUSTOM);

    public static final String SILKSCREEN  = "silkscreen";
    public static final String FABRICATION = "fabrication";
    public static final String COURTYARD   = "courtyard";

    /** A layer's purpose on a side of a pcb: `top-silkscreen`, `bottom-mask`. Copper is a stack's own, `top` and `inner-1`. */
    public static final Set<String> SIDE_PURPOSES = Set.of(SILKSCREEN, FABRICATION, COURTYARD, "mask", "paste", "adhesive");

    /** The layers of a pcb that stand on neither side: its drawings — its measures among them — and its notes, which no factory makes. */
    public static final String DOCUMENTATION = "documentation";
    public static final Set<String> SHEET_LAYERS = Set.of(DOCUMENTATION, "comments");

    /** The face of the pcb a surface pad stands on, when it is the one away from its part: an edge connector's back row. */
    public static final String FACE_FAR = "far";
    public static final Set<String> PAD_FACES = Set.of("near", FACE_FAR);

    public static final String OPENS_BOTH  = "both";
    public static final String OPENS_MASK  = "mask";
    public static final String OPENS_PASTE = "paste";
    public static final String OPENS_NONE  = "none";

    /** What a pad opens, when it says: the solder mask, the paste, both, or neither. */
    public static final Set<String> PAD_OPENINGS = Set.of(OPENS_BOTH, OPENS_MASK, OPENS_PASTE, OPENS_NONE);

    /** Which side's mask a via is left bare on, when it is both. */
    public static final String VIA_OPEN_BOTH = "both";

    /** Which side's solder mask a via is left bare on, when it says: covered on both is what it is otherwise. */
    /** Copper left out from under the solder mask, for solder to be run on it. */
    public static final String FINISH_BARE = "bare";

    /** What copper may be left as. */
    public static final Set<String> COPPER_FINISHES = Set.of(FINISH_BARE);

    public static final Set<String> VIA_OPENINGS = Set.of("top", "bottom", VIA_OPEN_BOTH);

    /** The layers a footprint draws an outline on: drawn on the mask or the paste, it opens them beside what its pads open. */
    public static final Set<String> OUTLINE_LAYERS = Set.of(SILKSCREEN, FABRICATION, COURTYARD, "copper", "mask", "paste",
            "adhesive", "documentation", "comments");

    /** What a footprint's field prints — the placed part's reference, or its value —, and where. */
    public static final String FIELD_REFERENCE = "reference";
    public static final String FIELD_VALUE = "value";
    public static final Set<String> FIELD_SUBJECTS = Set.of(FIELD_REFERENCE, FIELD_VALUE);
    public static final Set<String> FIELD_LAYERS   = Set.of(SILKSCREEN, FABRICATION);

    /** Which part of a text stands on its point: where its letters begin, their middle — the usual —, or where they end. */
    public static final String TEXT_START = "start";
    public static final String TEXT_MIDDLE = "middle";
    public static final String TEXT_END = "end";
    public static final Set<String> TEXT_ANCHORS = Set.of(TEXT_START, TEXT_MIDDLE, TEXT_END);

    /** The sides a part stands on. */
    public static final Set<String> SIDES = Set.of("top", "bottom");

    /** What a pcb's teardrops are said for. */
    public static final Set<String> TEARDROP_TARGETS = Set.of("round", "surface", "tracks");

    /** What a pcb offers sizes of: track widths, and via drills with their diameters. */
    /** What a placement may be to the copper beyond its pads: a 0 Ω link, its pads one piece of copper. */
    /** What a 0 Ω link's placement is: its pads are one piece of copper. */
    public static final String ROLE_LINK = "link";
    public static final Set<String> PLACEMENT_ROLES = Set.of(ROLE_LINK);

    public static final String TRACKS = "tracks";
    public static final String VIAS = "vias";
    public static final Set<String> SIZED = Set.of(TRACKS, VIAS);

    /**
     * The values a pcb is made and checked with: the least a pcb house makes — a gap, a track, a hole, a ring —, the
     * usual track and via new copper is laid with, and the copper and pcb thicknesses.
     */
    public static final String RULE_CLEARANCE = "clearance";
    public static final String RULE_LEAST_TRACK_WIDTH = "least-track-width";
    public static final String RULE_LEAST_DRILL = "least-drill";
    public static final String RULE_LEAST_VIA_DRILL = "least-via-drill";
    public static final String RULE_LEAST_VIA_RING = "least-via-ring";
    public static final String RULE_HOLE_CLEARANCE = "hole-clearance";
    public static final String RULE_EDGE_CLEARANCE = "edge-clearance";
    public static final String RULE_THERMAL_GAP = "thermal-gap";
    public static final String RULE_THERMAL_SPOKE_WIDTH = "thermal-spoke-width";
    public static final String RULE_TRACK_WIDTH = "track-width";
    public static final String RULE_VIA_DRILL = "via-drill";
    public static final String RULE_VIA_DIAMETER = "via-diameter";
    public static final String RULE_COPPER_THICKNESS = "copper-thickness";
    public static final String RULE_INNER_COPPER_THICKNESS = "inner-copper-thickness";
    public static final String RULE_PLATING_THICKNESS = "plating-thickness";
    public static final String RULE_PCB_THICKNESS = "pcb-thickness";
    public static final Set<String> RULES = Set.of(RULE_CLEARANCE, RULE_LEAST_TRACK_WIDTH, RULE_LEAST_DRILL, RULE_LEAST_VIA_DRILL,
            RULE_LEAST_VIA_RING, RULE_HOLE_CLEARANCE, RULE_EDGE_CLEARANCE, RULE_THERMAL_GAP, RULE_THERMAL_SPOKE_WIDTH, RULE_TRACK_WIDTH,
            RULE_VIA_DRILL, RULE_VIA_DIAMETER, RULE_COPPER_THICKNESS, RULE_INNER_COPPER_THICKNESS, RULE_PLATING_THICKNESS, RULE_PCB_THICKNESS);

    private PcbVocabulary() {
    }
}
