package org.jmouse.cad.pcb.routing;

import org.jmouse.cad.Point;
import org.jmouse.cad.pcb.drawing.Track;
import org.jmouse.cad.pcb.drawing.PcbDrawing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Bands that could not be laid, cut back to what holds them: the stretch a drag worked on taken off the line, what of the line ran
 * beyond it standing as it ran. Where the line's end was a pad that moved, it ends now at the node the band was held at — a pad, a
 * via, a junction, or, where the region the drag worked in cut the line, a free tip —, and a ratsnest line runs from there to the pad.
 */
final class BandCutting {

    private BandCutting() {
    }

    /** The pcb with each of the bands cut back: its tracks gone, what ran before and after the stretch worked on put back, each a track. */
    static PcbDrawing cut(PcbDrawing laid, Collection<Band> bands) {
        Set<Integer> gone = new HashSet<>();
        List<Track> kept = new ArrayList<>();
        for (Band band : bands) {
            gone.addAll(band.tracks());
            Track first = laid.tracks().get(band.track());
            for (List<Point> rest : List.of(band.head(), band.tail())) {
                if (rest.size() >= 2) {
                    kept.add(first.withPoints(rest));
                }
            }
        }
        List<Track> tracks = new ArrayList<>();
        for (int index = 0; index < laid.tracks().size(); index++) {
            if (!gone.contains(index)) {
                tracks.add(laid.tracks().get(index));
            }
        }
        tracks.addAll(kept);
        return laid.withTracks(tracks);
    }
}
