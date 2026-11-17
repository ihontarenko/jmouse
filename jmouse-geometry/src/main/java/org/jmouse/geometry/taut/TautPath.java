package org.jmouse.geometry.taut;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;

import java.util.List;
import java.util.Optional;

/**
 * 🪢 A path pulled tight among obstacles, as a rubber band is — the shortest way that goes round every obstacle on the side the
 * path does. What it keeps of a path is only which way round each obstacle it goes, so a detour round nothing comes straight, and
 * one round something stays round it on that side, never jumping to the other. Obstacles that moved onto the path a little way
 * push it ahead of them ({@link Detours}); moved far, they are moved a little at a time, the path pulled after each step.
 *
 * <ol>
 *   <li>the path laid round whatever of the obstacles it runs into, on the side it was on;</li>
 *   <li>the room between the obstacles round it cut into triangles ({@link FreeSpace});</li>
 *   <li>the sides of them it crosses, crossed back over cancelled ({@link Sleeve});</li>
 *   <li>the shortest way through those, pulled ({@link Funnel}).</li>
 * </ol>
 */
public final class TautPath {

    /** How far round a path the room it may be pulled through is looked at: past it, the window's edge is a wall. */
    private final double margin;

    public TautPath(double margin) {
        this.margin = margin;
    }

    /** How a path came out: pulled tight; laid round what it ran into but left loose; or squeezed — not to be laid at all. */
    public enum Tautness {
        TAUT, LOOSE, SQUEEZED
    }

    /** A path as it came out, and how. */
    public record Pull(List<Coordinate> path, Tautness tautness) {
    }

    /** The path pulled tight among the field's obstacles, its two ends where they are. */
    public Pull pulled(List<Coordinate> path, ObstacleField field) {
        Optional<List<Coordinate>> free = Detours.around(Paths.withoutRepeats(path), field);
        if (free.isEmpty()) {
            return new Pull(path, Tautness.SQUEEZED);
        }
        List<Coordinate> round = free.get();
        if (round.size() < 2) {
            return new Pull(round, Tautness.TAUT);
        }
        Envelope window = Paths.envelopeOf(round);
        window.expandBy(margin);
        Optional<FreeSpace> space = FreeSpace.of(field, window);
        if (space.isEmpty()) {
            return new Pull(round, Tautness.LOOSE);
        }
        Optional<List<Sleeve.Portal>> sleeve = Sleeve.through(space.get(), round, space.get().size());
        return sleeve.map(portals -> new Pull(Funnel.pulled(round.getFirst(), portals, round.getLast()), Tautness.TAUT))
                .orElseGet(() -> new Pull(round, Tautness.LOOSE));
    }
}
