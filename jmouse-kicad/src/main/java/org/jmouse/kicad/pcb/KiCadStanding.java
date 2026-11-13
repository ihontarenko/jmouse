package org.jmouse.kicad.pcb;

import org.jmouse.sexpression.node.SExpressionList;

import static org.jmouse.kicad.KiCadKeyword.ID;
import static org.jmouse.kicad.KiCadKeyword.LOCKED;
import static org.jmouse.kicad.KiCadKeyword.TSTAMP;
import static org.jmouse.kicad.KiCadKeyword.UUID;

/**
 * How an item of a board stands as KiCad wrote it: the identity a group names it by among its members, and whether it is locked.
 *
 * @param identity its {@code uuid} — KiCad 6's {@code tstamp} or {@code id} —; empty when it has none
 * @param lock     whether KiCad holds it where it stands
 */
public record KiCadStanding(String identity, KiCadLock lock) {

    /** An item KiCad says nothing of: no identity, free. */
    public static final KiCadStanding UNSAID = new KiCadStanding("", KiCadLock.FREE);

    public KiCadStanding {
        identity = identity == null ? "" : identity;
        lock = lock == null ? KiCadLock.FREE : lock;
    }

    /** How the item a list writes stands. */
    static KiCadStanding of(SExpressionList item) {
        String identity = item.wordOf(UUID).or(() -> item.wordOf(TSTAMP)).or(() -> item.wordOf(ID)).orElse("");
        return new KiCadStanding(identity, item.says(LOCKED) ? KiCadLock.LOCKED : KiCadLock.FREE);
    }
}
