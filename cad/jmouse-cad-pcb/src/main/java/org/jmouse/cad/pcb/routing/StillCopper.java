package org.jmouse.cad.pcb.routing;

import org.jmouse.geometry.taut.Obstacle;
import org.jmouse.geometry.taut.UnitedObstacles;
import org.locationtech.jts.geom.Envelope;

import java.util.BitSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * The copper a fence's corridors go round that stands still, grown and united once for each window searched — a window laid on a
 * grid of cells, so the windows a hand's every step asks for, near one another, are the very same window, and the copper united for
 * the first is the copper of the next. A corridor search asks a field for every leg it walks and every share of a carry it tries:
 * uniting every obstacle in its window again each time, or cutting one region united round them all, was what a carry spent its
 * time on.
 *
 * <p>Kept by a base fence for every fence made from it, by which of its obstacles a fence eases the gap of — what its ends stand on
 * —: those move with the ask, and are united apart each time with whatever else the fence keeps clear of.</p>
 */
final class StillCopper {

    /** How many windows' copper is kept: the windows near a hand, a few legs' worth. */
    private static final int KEPT_WINDOWS = 64;

    /** A window of the grid, for fences easing these obstacles. */
    private record WindowKey(BitSet eased, double left, double top, double right, double bottom) {
    }

    private final Map<WindowKey, UnitedObstacles> kept = new LinkedHashMap<>(KEPT_WINDOWS, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<WindowKey, UnitedObstacles> eldest) {
            return size() > KEPT_WINDOWS;
        }
    };

    /** A window grown out to the lines of a grid of cells this wide: the window a search near it is made in too. */
    static Envelope onGrid(Envelope window, double cell) {
        if (cell <= 0) {
            return new Envelope(window);
        }
        return new Envelope(Math.floor(window.getMinX() / cell) * cell, Math.ceil(window.getMaxX() / cell) * cell,
                            Math.floor(window.getMinY() / cell) * cell, Math.ceil(window.getMaxY() / cell) * cell);
    }

    /**
     * The still copper of a window on the grid ({@link #onGrid}), united: the one united before for fences easing the same obstacles,
     * else the obstacles within it united now.
     *
     * @param eased       the base's obstacles a fence eases the gap of, left out
     * @param obstaclesIn the still obstacles within a window, grown round as the fence keeps them and cut to it
     */
    synchronized UnitedObstacles unitedOver(BitSet eased, Envelope window, Function<Envelope, List<Obstacle>> obstaclesIn) {
        WindowKey key = new WindowKey((BitSet) eased.clone(), window.getMinX(), window.getMinY(), window.getMaxX(), window.getMaxY());
        UnitedObstacles known = kept.get(key);
        if (known == null) {
            known = UnitedObstacles.of(obstaclesIn.apply(window), KeptOut.SLACK, Plane.FACTORY);
            kept.put(key, known);
        }
        return known;
    }
}
