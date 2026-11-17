package org.jmouse.cad.pcb.routing;

import java.util.Locale;

/**
 * What a place copper lines end or meet at is ({@link CopperNode}). A pad and a via are nodes whether anything meets them or not;
 * the rest are where tracks and arcs end: the rest of a line runs on from a plain joint, branches at a junction, stops at a tip.
 */
public enum CopperNodeKind {

    /** A pad: a line's end on it holds there, whatever is moved. */
    PAD,
    /** A via: the lines of each layer it joins meet in it. */
    VIA,
    /** Three or more lines meeting, or one ending on another away from its ends — a T. */
    JUNCTION,
    /** An arc's end, and the lines ending on it. */
    ARC_END,
    /** A track's end that nothing meets: the tip of a line left open, a line cut back to it. */
    TRACK_TIP,
    /** Two tracks joined end to end and nothing else: one line to a person, running on through it. */
    JOINT;

    /** How it is written: {@code track-tip}. */
    public String word() {
        return name().toLowerCase(Locale.ROOT).replace('_', '-');
    }

    /** The one a word names; refused for a word that names none. */
    public static CopperNodeKind of(String word) {
        return valueOf(word.strip().toUpperCase(Locale.ROOT).replace('-', '_'));
    }
}
