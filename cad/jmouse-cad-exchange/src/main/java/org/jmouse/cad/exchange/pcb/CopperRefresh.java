package org.jmouse.cad.exchange.pcb;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

/**
 * 🫗 When a pcb's copper is poured and checked again as the reader works — the reader's choice (`pcb/laying` copper), each the
 * moments it pours at:
 *
 * <ul>
 *   <li>{@link #ON_CHANGE} — as a gesture goes, from what it shows, and after every step kept;</li>
 *   <li>{@link #ON_RELEASE} — after every step kept: what a gesture shows is poured once it is let go;</li>
 *   <li>{@link #ON_DEMAND} — only when asked, as KiCad pours on B; the copper stands as it was meanwhile, out of date.</li>
 * </ul>
 *
 * <p>The editor tells the moment a layout is asked about ({@link LayoutMoment}); this says whether that moment is poured.</p>
 */
public enum CopperRefresh {

    ON_CHANGE(EnumSet.of(LayoutMoment.SHOWN, LayoutMoment.KEPT, LayoutMoment.ASKED)),
    ON_RELEASE(EnumSet.of(LayoutMoment.KEPT, LayoutMoment.ASKED)),
    ON_DEMAND(EnumSet.of(LayoutMoment.ASKED));

    private final Set<LayoutMoment> poured;

    CopperRefresh(Set<LayoutMoment> poured) {
        this.poured = poured;
    }

    /** Whether a layout asked about at this moment has its copper poured again. */
    public boolean pours(LayoutMoment moment) {
        return poured.contains(moment);
    }

    /** The one a word names — {@code on-change}, {@code on-release}, {@code on-demand} —; none, on release. */
    public static CopperRefresh of(String word) {
        return word == null ? ON_RELEASE : valueOf(word.toUpperCase(Locale.ROOT).replace('-', '_'));
    }
}
