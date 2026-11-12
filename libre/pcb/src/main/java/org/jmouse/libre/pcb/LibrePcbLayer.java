package org.jmouse.libre.pcb;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** The layers the format itself gives a meaning, as it names them: what is a board's edge, and what is copper. */
public final class LibrePcbLayer {

    public static final String TOP_COPPER    = "top_cu";
    public static final String BOTTOM_COPPER = "bot_cu";
    public static final String OUTLINES      = "brd_outlines";
    public static final String CUTOUTS       = "brd_cutouts";
    public static final String PLATED_CUTOUTS = "brd_plated_cutouts";

    /** A symbol's area that is there to be grabbed by the mouse, and is never drawn. */
    public static final String SYMBOL_GRAB_AREA = "sym_hidden_grab_areas";

    private static final String TOP_OPENING    = "top_";
    private static final String BOTTOM_OPENING = "bot_";
    private static final Pattern INNER_COPPER = Pattern.compile("in(\\d+)_cu");

    private LibrePcbLayer() {
    }

    public static boolean isEdge(String layer) {
        return OUTLINES.equals(layer) || CUTOUTS.equals(layer) || PLATED_CUTOUTS.equals(layer);
    }

    public static boolean isTop(String layer) {
        return layer.startsWith(TOP_OPENING);
    }

    public static boolean isBottom(String layer) {
        return layer.startsWith(BOTTOM_OPENING);
    }

    /** Which inner copper layer it is, from one; empty for a layer that is none. */
    public static Optional<Integer> innerCopper(String layer) {
        Matcher matcher = INNER_COPPER.matcher(layer);
        return matcher.matches() ? Optional.of(Integer.valueOf(matcher.group(1))) : Optional.empty();
    }
}
