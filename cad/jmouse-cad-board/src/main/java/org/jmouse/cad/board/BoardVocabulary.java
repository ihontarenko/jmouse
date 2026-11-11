package org.jmouse.cad.board;

import java.util.Set;

/** The words a board's and a footprint's lines are written with, as data the grammars name. */
public final class BoardVocabulary {

    /** {@code circuit rc-discharging}: the circuit a board is laid out for. */
    public static final String CIRCUIT = "circuit";

    /** How a pad is made. */
    public static final Set<String> PAD_TYPES = Set.of("through-hole", "surface", "connector", "unplated-hole");

    /** The shape a pad is cut in. */
    public static final Set<String> PAD_SHAPES = Set.of("circle", "rectangle", "oval", "trapezoid", "rounded-rectangle", "custom");

    /** The layers a footprint draws an outline on. */
    public static final Set<String> OUTLINE_LAYERS = Set.of("silkscreen", "fabrication", "courtyard", "copper");

    /** What a footprint's field prints, and where. */
    public static final Set<String> FIELD_SUBJECTS = Set.of("reference", "value");
    public static final Set<String> FIELD_LAYERS   = Set.of("silkscreen", "fabrication");

    /** The sides a part stands on. */
    public static final Set<String> SIDES = Set.of("top", "bottom");

    /** What a board's teardrops are said for. */
    public static final Set<String> TEARDROP_TARGETS = Set.of("round", "surface", "tracks");

    /** What a board offers sizes of: track widths, and via drills with their diameters. */
    public static final String TRACKS = "tracks";
    public static final String VIAS = "vias";
    public static final Set<String> SIZED = Set.of(TRACKS, VIAS);

    /**
     * The values a board is made and checked with: the least a board house makes — a gap, a track, a hole, a ring —, the
     * usual track and via new copper is laid with, and the copper and board thicknesses.
     */
    public static final Set<String> RULES = Set.of("clearance", "least-track-width", "least-drill", "least-via-drill",
            "least-via-ring", "hole-clearance", "edge-clearance", "thermal-gap", "thermal-spoke-width", "track-width", "via-drill", "via-diameter",
            "copper-thickness", "inner-copper-thickness", "plating-thickness", "board-thickness");

    private BoardVocabulary() {
    }
}
