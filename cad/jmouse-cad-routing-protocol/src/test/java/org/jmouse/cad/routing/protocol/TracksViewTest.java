package org.jmouse.cad.routing.protocol;

import org.jmouse.cad.pcb.drawing.PcbDrawing.Point;
import org.jmouse.cad.pcb.drawing.PcbDrawing.Track;
import org.jmouse.cad.routing.protocol.RoutingAsks.TrackChange;
import org.jmouse.cad.routing.protocol.RoutingAsks.TracksView;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** An answer's tracks said as what changed in the tracks it was asked of: only those, at their indexes, and how many there are now. */
class TracksViewTest {

    private static Track track(double x) {
        return new Track("top", 0.2, 0, List.of(new Point(x, 0), new Point(x, 10)), null, null);
    }

    @Test
    void onlyTheTracksThatChangedAreSaid() {
        List<Track> before = List.of(track(0), track(1), track(2));
        List<Track> after = List.of(before.get(0), track(5), track(2));

        TracksView view = TracksView.between(before, after);

        assertEquals(3, view.count());
        assertEquals(List.of(new TrackChange(1, after.get(1))), view.changed());
    }

    @Test
    void tracksAddedAreSaidAndTracksRemovedAreCounted() {
        List<Track> before = List.of(track(0), track(1), track(2));

        assertEquals(new TracksView(4, List.of(new TrackChange(3, track(9)))), TracksView.between(before, List.of(track(0), track(1), track(2), track(9))));
        assertEquals(new TracksView(2, List.of()), TracksView.between(before, List.of(track(0), track(1))));
    }

    @Test
    void aGestureThatCannotMoveSaysNone() {
        assertNull(TracksView.NONE.count());
        assertNull(TracksView.NONE.changed());
    }
}
