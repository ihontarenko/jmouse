package org.jmouse.cad.pcb.routing;

import org.jmouse.cad.pcb.drawing.PcbDrawing;
import org.jmouse.cad.pcb.drawing.PcbDrawing.Point;

import org.jmouse.cad.pcb.geometry.CopperNets;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * 🪢 Parts dragged — moved, turned, put on the other side — and what that does to copper. As they go, only the copper standing wholly
 * on them goes with them ({@link RigidCopper}): nothing is traced while the hand moves. Let go, the lines stay where they ran
 * ({@link PartDragMode#DETACH}), or ({@link PartDragMode#RECONNECT}) every line they pull or push is a rubber band ({@link DragBands}),
 * pulled tight along the way among everything else ({@link BandPulling}), its joints that are free to slide moved to where its lines
 * are shortest ({@link SlidingJoints}), and laid at the reader's angles ({@link OctilinearBands}); a band that cannot be laid is cut
 * back to what holds it ({@link BandCutting}), never the whole move refused for it.
 */
final class PartDragging {

    /**
     * A drag worked out: what it leaves, what of it could not be laid clear, and the lines it pulls — told by their first tracks —:
     * those with an end on a pad it moved, as against those it only pushes.
     */
    private record Worked(PartDrag drag, OctilinearBands.Laid laid, Set<Integer> pulled) {

        boolean pulls(Band band) {
            return pulled.contains(band.track());
        }
    }

    private final RoutingEngine engine;
    private final BandPulling   pulling;
    private Optional<Worked>    last = Optional.empty();

    PartDragging(RoutingEngine engine, RoutingSettings settings) {
        this.engine = engine;
        this.pulling = new BandPulling(settings.banding());
    }

    /**
     * What a drag starts from, worked out alike for a frame and for a release: the board before it, the motion, the moved parts' pads,
     * the copper standing wholly on them carried with them, the tracks that go whole with a part, and the lines it pulls or pushes.
     */
    private record Carrying(RoutingWorld before, DragMotion motion, List<RigidCopper.MovedPad> pads, PcbDrawing carried,
                            Map<Integer, String> whole, List<Band> bands) {
    }

    private Carrying carrying(PartDrag drag) {
        RoutingWorld before = engine.worldOf(drag.before());
        DragMotion motion = DragMotion.of(drag.before(), drag.moved());
        List<RigidCopper.MovedPad> pads = RigidCopper.movedPads(before, motion);
        PcbDrawing carried = RigidCopper.carried(before, drag.moved(), pads, motion);
        if (pads.isEmpty()) {
            return new Carrying(before, motion, pads, carried, Map.of(), List.of());
        }
        Map<Integer, String> whole = RigidCopper.wholeTracks(before, drag.moved(), pads, motion);
        return new Carrying(before, motion, pads, carried, whole, DragBands.of(before, carried, motion, pads, whole.keySet()));
    }

    /**
     * The pcb a frame of the drag shows — worked out within a frame —: the parts where they are, the copper standing wholly on them
     * with them. Detached, the lines as they ran. Reconnected, each line the drag pulls stretched straight to the pad as it now stands,
     * its other points kept: a part never stands away from the copper it holds. The rubber bands round what the lines meet are laid
     * on release alone: laid on every frame they cost 0.1–1 s a frame of a board as Linear 500 W (measured 2026-11-16), and a part
     * that waits for them lags its hand.
     */
    PcbDrawing following(PartDrag drag) {
        Carrying carrying = carrying(drag);
        if (drag.mode() == PartDragMode.DETACH) {
            return carrying.carried();
        }
        RoutingWorld stretched = engine.draggedWorldOf(carrying.carried(), drag);
        for (Band band : carrying.bands()) {
            if (band.moving()) {
                stretched = band.laidOn(stretched, band.wholeAlong(band.pathAt(carrying.motion(), 1)));
            }
        }
        return stretched.pcb();
    }

    /** The lines the drag pulls or pushes, laid as rubber bands, before any that could not be laid is cut back. */
    PcbDrawing banded(PartDrag drag) {
        return worked(drag).laid().pcb();
    }

    /**
     * A pcb a release leaves, kept: detached, the lines as they ran; reconnected, laid again, a band that cannot be laid — squeezed,
     * not clear of what it meets, joining another net — cut back to what holds it. Refused only where copper of two nets would meet
     * that no band makes meet: a pad put down on another net's copper.
     */
    RoutingEngine.Release released(PartDrag drag) {
        return switch (drag.mode()) {
            case DETACH -> {
                PcbDrawing kept = carrying(drag).carried();
                yield releaseOf(kept, engine.worldOf(kept).meetingsNotIn(engine.worldOf(drag.before())));
            }
            case RECONNECT -> reconnected(drag);
        };
    }

    /** The pcb kept, or — where copper of two nets would meet because of the move — refused, said where. */
    private RoutingEngine.Release releaseOf(PcbDrawing kept, List<String> meetings) {
        return meetings.isEmpty() ? new RoutingEngine.Release.Laid(kept)
                : new RoutingEngine.Release.Refused(meetings, engine.board().checks().namedMeetings());
    }

    /**
     * The bands laid; a line the move pulls that could not be laid — or whose own copper would touch another net's — cut back to what
     * holds it, a ratsnest line from there to its pad; a line it only pushes that could not be laid standing as it ran. What then
     * still meets another net is the move's own doing — a pad put down on a via —, and the release refuses it.
     */
    private RoutingEngine.Release reconnected(PartDrag drag) {
        Worked worked = worked(drag);
        OctilinearBands.Laid laid = worked.laid();
        RoutingWorld before = engine.worldOf(drag.before());
        Set<Band> failed = new LinkedHashSet<>();
        laid.faults().forEach(fault -> failed.add(fault.band()));
        Set<Integer> touching = new HashSet<>();
        for (CopperNets.Joined join : engine.worldOf(laid.pcb()).joinsNotIn(before)) {
            touching.addAll(tracksTouchingAnotherNet(before.nets(), join.chain()));
        }
        laid.bands().stream().filter(band -> band.tracks().stream().anyMatch(touching::contains)).forEach(failed::add);
        PcbDrawing standing = laid.pcb();
        for (Band pushed : failed.stream().filter(band -> !worked.pulls(band)).toList()) {
            standing = asItRan(standing, drag.before(), pushed);
        }
        List<Band> cut = failed.stream().filter(worked::pulls).toList();
        // Said before anything is cut, so the copper is named as the reader sees it: a meeting whose chain the cut breaks is parted.
        Set<Integer> parted = new HashSet<>();
        cut.forEach(band -> parted.addAll(band.tracks()));
        RoutingWorld uncut = engine.worldOf(standing);
        List<String> meetings = uncut.joinsNotIn(before).stream()
                .filter(join -> join.chain().stream().noneMatch(member -> member.kind() == CopperNets.MemberKind.TRACK && parted.contains(member.index())))
                .map(uncut::meetingOf).toList();
        PcbDrawing kept = cut.isEmpty() ? standing : relaidAfresh(BandCutting.cut(standing, cut), standing, cut, drag);
        if (meetings.isEmpty() && !cut.isEmpty()) {
            // Two nets that still meet another way once the chain is broken: said as the board stands after the cut.
            meetings = engine.worldOf(kept).meetingsNotIn(before);
        }
        return releaseOf(kept, meetings);
    }

    /**
     * The lines cut back laid afresh where a way can be found: from the pad the band ran to, to the node it was cut back to, searched
     * for through the free room's corridor — which goes whichever way round is open, where the band kept the way round it had —, the
     * width and layer the line had. Where none is found, the line stays cut back, a ratsnest line from its node to the pad.
     */
    private PcbDrawing relaidAfresh(PcbDrawing cutBack, PcbDrawing uncut, List<Band> cut, PartDrag drag) {
        PcbDrawing board = cutBack;
        for (Band band : cut) {
            List<Point> path = band.path();
            if (path.size() < 2) {
                continue;
            }
            Point from = path.getFirst();
            Point to = path.getLast();
            TrackRouter.RoutedLeg leg = TrackRouter.routedLeg(TrackRouter.LegRequest.of(engine.worldOf(board), OptionalInt.empty(), band.layer(),
                    band.net(), band.width(), from, to, drag.angleStep()));
            if (leg.reached()) {
                List<PcbDrawing.Track> tracks = new ArrayList<>(board.tracks());
                tracks.add(uncut.tracks().get(band.track()).withPoints(Lines.joined(List.of(from), leg.points())));
                board = board.withTracks(tracks);
            }
        }
        return board;
    }

    /** The pcb with a band's tracks back where they ran before the drag. */
    private static PcbDrawing asItRan(PcbDrawing pcb, PcbDrawing before, Band band) {
        PcbDrawing standing = pcb;
        for (int index : band.tracks()) {
            standing = standing.withTrackPoints(index, before.tracks().get(index).points());
        }
        return standing;
    }

    /** The tracks of a chain of copper that touch, next in the chain, a piece of another net — as the board before named them. */
    private static Set<Integer> tracksTouchingAnotherNet(CopperNets named, List<CopperNets.Member> chain) {
        Set<Integer> touching = new HashSet<>();
        for (int position = 1; position < chain.size(); position++) {
            CopperNets.Member one = chain.get(position - 1);
            CopperNets.Member other = chain.get(position);
            Optional<String> oneNet = netOf(named, one);
            Optional<String> otherNet = netOf(named, other);
            if (oneNet.isPresent() && otherNet.isPresent() && !oneNet.equals(otherNet)) {
                List.of(one, other).stream().filter(member -> member.kind() == CopperNets.MemberKind.TRACK).forEach(member -> touching.add(member.index()));
            }
        }
        return touching;
    }

    /** The net a piece of copper is on, as named; none for a piece of no net, or of a kind no band is. */
    private static Optional<String> netOf(CopperNets named, CopperNets.Member member) {
        return switch (member.kind()) {
            case PAD -> named.padNets().get(member.index());
            case TRACK -> named.trackNets().get(member.index());
            case ARC -> named.arcNets().get(member.index());
            case VIA -> named.viaNets().get(member.index());
            case DRAWN -> named.drawnNets().get(member.index());
            case ZONE -> Optional.empty();
        };
    }

    /** The drag worked out, once for the bands asked for and the release that follows them. */
    private Worked worked(PartDrag drag) {
        if (last.isPresent() && last.get().drag().equals(drag)) {
            return last.get();
        }
        Worked worked = workedOut(drag);
        last = Optional.of(worked);
        return worked;
    }

    /**
     * The bands pulled along the drag, every one that folds at a corner holding it worked on further and the drag pulled again from
     * the start, until none folds: a corner holds a band only where the line runs on through it.
     */
    private List<Band> pulledUnfolded(List<Band> bands, BandField field, DragMotion motion, double farthest) {
        // Where a band would fold even pulled straight between where its ends are let go, it is let go of there before it is pulled.
        List<Band> asked = bands.stream().map(band -> band.unfoldedAsLetGo(motion)).toList();
        List<Band> pulled = pulling.pulled(asked, field, motion, farthest);
        while (true) {
            List<Band> further = new ArrayList<>(asked);
            boolean folded = false;
            for (int index = 0; index < pulled.size(); index++) {
                Optional<Band> letGo = pulled.get(index).letGoWhereFolded();
                if (letGo.isPresent()) {
                    further.set(index, letGo.get());
                    folded = true;
                }
            }
            if (!folded) {
                return pulled;
            }
            asked = further;
            pulled = pulling.pulled(asked, field, motion, farthest);
        }
    }

    private Worked workedOut(PartDrag drag) {
        Carrying carrying = carrying(drag);
        DragMotion motion = carrying.motion();
        List<RigidCopper.MovedPad> pads = carrying.pads();
        PcbDrawing carried = carrying.carried();
        if (pads.isEmpty()) {
            return new Worked(drag, new OctilinearBands.Laid(carried, List.of(), List.of()), Set.of());
        }
        Map<Integer, String> whole = carrying.whole();
        List<Band> bands = carrying.bands();
        Set<Integer> pulls = new HashSet<>();
        bands.stream().filter(Band::moving).forEach(band -> pulls.add(band.track()));
        RoutingWorld standing = engine.draggedWorldOf(carried, drag);
        BandField field = new BandField(standing, motion, bands, whole);
        List<Band> pulled = pulledUnfolded(bands, field, motion, motion.farthest(pads.stream().map(RigidCopper.MovedPad::pad).toList()));
        SlidingJoints.Slid slid = new SlidingJoints(standing, pulling, drag.angleStep()).slid(pulled.stream().map(band -> band.atRest(motion)).toList());
        return new Worked(drag, OctilinearBands.laid(engine.draggedWorldOf(slid.pcb(), drag), slid.bands(), drag), Set.copyOf(pulls));
    }
}
