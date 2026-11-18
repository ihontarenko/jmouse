package org.jmouse.cad.pcb.routing;

import org.jmouse.cad.pcb.drawing.PcbDrawing;
import org.jmouse.cad.pcb.drawing.PcbDrawing.Point;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * Chosen tracks' corners rounded, as a person sees the line: a stretch chosen is its line chosen ({@link JoinedTracks}) — its own corners
 * and the corners where it joins the rest. A board read from KiCad keeps one track a stretch, so its corners are all joints: the stretches
 * of a chosen line that run on one layer at one width become one track, its corners rounded as far as their gaps allow. KiCad gets it back as
 * stretches and arcs. The merged track stands where the first of its stretches stood; what was chosen is said again by where it now
 * stands, so the choice stays on the same copper.
 *
 * @param rounding the radius every chosen corner is rounded to; nothing the gaps do not allow
 */
public record CornerRounding(RoutingWorld world, double rounding) {

    /** The pcb with its chosen tracks' corners rounded, and where each chosen track now stands. */
    public record Rounded(PcbDrawing pcb, List<Integer> chosen) {
    }

    /** One run of a line's chosen stretches made one track: where it stands, the stretches it takes the place of, its points. */
    private record Merge(int at, List<Integer> gone, PcbDrawing.Track track) {
    }

    public Rounded rounded(List<Integer> chosen) {
        PcbDrawing pcb = world.pcb();
        TreeSet<Integer> wanted = new TreeSet<>(chosen);
        List<Merge> merges = new ArrayList<>();
        TreeSet<Integer> seen = new TreeSet<>();
        for (int index : wanted) {
            if (seen.contains(index) || index < 0 || index >= pcb.tracks().size()) {
                continue;
            }
            JoinedTracks.JoinedLine line = JoinedTracks.joinedLine(world, index);
            TreeSet<Integer> wholeLine = new TreeSet<>();
            line.members().forEach(member -> wholeLine.add(member.index()));
            seen.addAll(wholeLine);
            merges.addAll(mergesOf(line, wholeLine));
        }
        return written(pcb, wanted, merges);
    }

    /** The runs of a line's members that are free and alike — one layer, one width —, each made one rounded track. */
    private List<Merge> mergesOf(JoinedTracks.JoinedLine line, TreeSet<Integer> wanted) {
        PcbDrawing pcb = world.pcb();
        List<Merge> merges = new ArrayList<>();
        int from = 0;
        while (from < line.members().size()) {
            int to = from;
            if (takes(pcb, wanted, line.members().get(from).index())) {
                PcbDrawing.Track first = pcb.tracks().get(line.members().get(from).index());
                while (to + 1 < line.members().size() && takes(pcb, wanted, line.members().get(to + 1).index())
                        && alike(first, pcb.tracks().get(line.members().get(to + 1).index()))) {
                    to++;
                }
                merges.add(mergeOf(line, from, to));
            }
            from = to + 1;
        }
        return merges;
    }

    private Merge mergeOf(JoinedTracks.JoinedLine line, int from, int to) {
        PcbDrawing pcb = world.pcb();
        int end = to + 1 < line.members().size() ? line.starts().get(to + 1) : line.points().size() - 1;
        // A stretch alone keeps its own points and way; a run of them takes the line's, its joints where their stretches meet.
        List<Point> points = from == to ? pcb.tracks().get(line.members().get(from).index()).points()
                : line.points().subList(line.starts().get(from), end + 1);
        List<Integer> indexes = line.members().subList(from, to + 1).stream().map(JoinedTracks.Member::index).sorted().toList();
        int at = indexes.getFirst();
        PcbDrawing.Track first = pcb.tracks().get(line.members().get(from).index());
        TrackFence fence = world.fenceOfTrack(first.withPoints(points), at, world.netOfTrack(at));
        // Rounding changes copper at corners alone: a line with none takes the radius asked, for the corners it may yet be given.
        double fits = fence.roundingThatFits(points, rounding, first.layer(), world.settings().routing().roundingHalvings());
        PcbDrawing.Track merged = first.withPoints(points).withRounding(fits);
        return new Merge(at, indexes.subList(1, indexes.size()), merged);
    }

    private static boolean takes(PcbDrawing pcb, TreeSet<Integer> wanted, int index) {
        return wanted.contains(index) && !RoutingEngine.held(pcb, index);
    }

    private static boolean alike(PcbDrawing.Track one, PcbDrawing.Track other) {
        return one.layer().equals(other.layer()) && one.width() == other.width();
    }

    /** The tracks with each merge in place, the stretches it took gone; and where every chosen track now stands. */
    private static Rounded written(PcbDrawing pcb, TreeSet<Integer> wanted, List<Merge> merges) {
        Map<Integer, PcbDrawing.Track> placed = new HashMap<>();
        TreeSet<Integer> gone = new TreeSet<>();
        Map<Integer, Integer> mergedInto = new HashMap<>();
        for (Merge merge : merges) {
            placed.put(merge.at(), merge.track());
            gone.addAll(merge.gone());
            merge.gone().forEach(index -> mergedInto.put(index, merge.at()));
        }
        List<PcbDrawing.Track> tracks = new ArrayList<>();
        for (int index = 0; index < pcb.tracks().size(); index++) {
            if (!gone.contains(index)) {
                tracks.add(placed.getOrDefault(index, pcb.tracks().get(index)));
            }
        }
        // Where an old track now stands: as many places earlier as tracks gone before it.
        TreeSet<Integer> chosen = new TreeSet<>();
        for (int index : wanted) {
            int standing = mergedInto.getOrDefault(index, index);
            if (standing >= 0 && standing < pcb.tracks().size()) {
                chosen.add(standing - gone.headSet(standing).size());
            }
        }
        return new Rounded(pcb.withTracks(tracks), List.copyOf(chosen));
    }
}
