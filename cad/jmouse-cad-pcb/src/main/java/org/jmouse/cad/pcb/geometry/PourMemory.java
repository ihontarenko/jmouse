package org.jmouse.cad.pcb.geometry;

import org.jmouse.cad.pcb.drawing.PcbDrawing;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.jmouse.geometry.Areas;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * What each zone was last poured from, and what it poured, so a zone is poured again only where a change reached it. A pour at a
 * point depends only on what stands within a little of it — the copper it keeps clear of, the pads it reliefs, and the narrowest
 * copper it may leave, which shrinks the pour and grows it back —; so where the pieces it is made of differ from last time, the
 * pour is worked out again inside the boxes of the pieces that changed, grown by that reach, and the rest is kept as it was.
 *
 * <p>Owned by what pours a pcb again and again — the engine answering an editor's every change — and passed to each pour
 * ({@link ZonePours}); a pour made once, for an export, keeps nothing ({@link #none()}). Kept by the zone itself: a zone of
 * another pcb that happens to be the same differs in its pieces, and is worked out where they differ — never wrongly, at
 * worst all over again. Safe to share between threads.</p>
 */
public final class PourMemory {

    private static final GeometryFactory GEOMETRY = new GeometryFactory();

    /** A piece is reworked where it changed only while the boxes changed cover less than this share of the zone: else all of it is. */
    private static final double MOST_REWORKED = 0.5;

    /**
     * How far past the region the new copper is taken, so it overlaps the copper kept and leaves no seam: far below any reach of a
     * change — the domain is a whole reach past the region —, far above what an overlay rounds.
     */
    private static final double SEAM_OVERLAP = 1e-3;

    /**
     * One thing a pour is made of: what it is, written so that equal inputs compare equal ({@code input}), the box its copper can
     * reach, and its copper, made only when a pour inside that box needs it.
     */
    record Piece(Role role, Object input, Envelope reach, Supplier<Geometry> copper) {

        Key key() {
            return new Key(role, input);
        }
    }

    /** What a piece does to the pour: copper of another net kept clear of, a pad of its own relieved round, a thermal's spokes. */
    enum Role {
        OTHER, THERMAL, SPOKES
    }

    /** A piece as it compares: its role and its input. */
    record Key(Role role, Object input) {
    }

    /**
     * What a whole pour depends on beyond its pieces — the zone, the area it may fill —, and how far a change of a piece reaches
     * into the pour round it: where the frame differs from last time, the zone is poured all over again.
     *
     * @param reach how far from a piece's copper the pour can change with it: what the pour is shrunk by and grown back by, each
     *              with what an offset followed in chords may stand off
     */
    record Frame(PcbDrawing.Zone zone, Object area, double reach) {
    }

    /** A zone's last pour: what framed it, the pieces it was made of by their keys, and what it poured before islands were taken off. */
    private record Kept(Frame frame, Map<Key, Piece> pieces, Geometry opened) {
    }

    private final Map<PcbDrawing.Zone, Kept> kept;

    private PourMemory(int zonesKept) {
        this.kept = new LinkedHashMap<>(Math.max(1, zonesKept), 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<PcbDrawing.Zone, Kept> eldest) {
                return size() > zonesKept;
            }
        };
    }

    /** A memory of the last pours of this many zones, the least recently poured let go first. */
    public static PourMemory keeping(int zones) {
        if (zones < 0) {
            throw new IllegalArgumentException("A pour memory keeps no fewer than no zones: " + zones);
        }
        return new PourMemory(zones);
    }

    /** No memory: every zone poured all over again — a pour made once. */
    public static PourMemory none() {
        return new PourMemory(0);
    }

    /**
     * The zone's pour, before its islands are taken off: worked out where its pieces changed since last time, or all over again.
     *
     * @param area     the area the zone may fill: its own, inside the pcb, out of its keepouts
     * @param pieces   what it is made of now
     * @param openedIn the pour worked out inside an area, from the pieces that reach it
     */
    Geometry opened(Frame frame, Geometry area, List<Piece> pieces, OpenedIn openedIn) {
        Map<Key, Piece> now = new LinkedHashMap<>();
        pieces.forEach(piece -> now.putIfAbsent(piece.key(), piece));
        Kept before;
        synchronized (kept) {
            before = kept.get(frame.zone());
        }
        Geometry opened = before == null || !before.frame().equals(frame) ? openedIn.within(area, now.values())
                : reworked(before, now, area, frame.reach(), openedIn);
        synchronized (kept) {
            kept.put(frame.zone(), new Kept(frame, now, opened));
        }
        return opened;
    }

    /** The pour worked out again inside the boxes of the pieces that changed, grown by what a change reaches; kept elsewhere. */
    private static Geometry reworked(Kept before, Map<Key, Piece> now, Geometry area, double reach, OpenedIn openedIn) {
        Set<Key> changed = new HashSet<>(now.keySet());
        changed.addAll(before.pieces().keySet());
        Set<Key> both = new HashSet<>(now.keySet());
        both.retainAll(before.pieces().keySet());
        changed.removeAll(both);
        if (changed.isEmpty()) {
            return before.opened();
        }
        // The region: where the pour can change, each changed piece's box grown by the reach. The domain: where it is worked
        // out, grown by the reach once more, so the pour inside the region is as a whole pour would have it — boxes grown
        // exactly, never a rounded buffer, which a chord cuts short at every corner.
        List<Envelope> changedBoxes = changed.stream().map(key -> (now.containsKey(key) ? now.get(key) : before.pieces().get(key)).reach()).toList();
        Geometry region = boxesGrown(changedBoxes, reach);
        Envelope whole = area.getEnvelopeInternal();
        if (region.getArea() > whole.getArea() * MOST_REWORKED) {
            return openedIn.within(area, now.values());
        }
        Geometry domain = boxesGrown(changedBoxes, 2 * reach);
        Envelope domainBox = domain.getEnvelopeInternal();
        List<Piece> near = now.values().stream().filter(piece -> piece.reach().intersects(domainBox)).toList();
        Geometry local = openedIn.within(Areas.common(area, domain), near);
        // The new copper is taken a hair past the region, so it overlaps what is kept: two parts that only met along the region's
        // edge left a seam — a crack of no area, its points in the middle of the copper — wherever the overlay rounded the edge.
        Geometry overlapping = boxesGrown(changedBoxes, reach + SEAM_OVERLAP);
        return Areas.union(Areas.difference(before.opened(), region), Areas.common(local, overlapping));
    }

    /** The union of boxes, each grown by a distance on every side. */
    private static Geometry boxesGrown(List<Envelope> boxes, double distance) {
        List<Geometry> grown = new ArrayList<>();
        for (Envelope box : boxes) {
            Envelope larger = new Envelope(box);
            larger.expandBy(distance);
            grown.add(GEOMETRY.toGeometry(larger));
        }
        return Areas.unionOf(grown, GEOMETRY);
    }

    /** The pour worked out inside an area, from the pieces that may reach it. */
    @FunctionalInterface
    interface OpenedIn {
        Geometry within(Geometry area, Collection<Piece> pieces);
    }

    /** The copper of the pieces of a role that reach an area's box, made now. */
    static List<Geometry> copperOf(Collection<Piece> pieces, Role role, Envelope reach) {
        return pieces.stream().filter(piece -> piece.role() == role && piece.reach().intersects(reach)).map(piece -> piece.copper().get()).toList();
    }
}
