package org.jmouse.cad.pcb.protocol;

import org.jmouse.cad.pcb.routing.RoutingEngine;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.function.BiFunction;

/**
 * 🛤 Each gesture an editor asks of an open board: the request it sends and the call that answers it. Named as the editor names
 * them — {@code leg}, {@code tune-track} —, so a request arriving by name, on a socket or in a page, finds its answer here.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public enum RoutingGesture {

    LEG(RoutingAsks.LegRequest.class, (engine, request) -> RoutingAsks.leg(engine, (RoutingAsks.LegRequest) request)),
    SEGMENT(RoutingAsks.SegmentRequest.class, (engine, request) -> RoutingAsks.segment(engine, (RoutingAsks.SegmentRequest) request)),
    CORNER(RoutingAsks.CornerRequest.class, (engine, request) -> RoutingAsks.corner(engine, (RoutingAsks.CornerRequest) request)),
    FOLLOWING(RoutingAsks.CarriedRequest.class, (engine, request) -> RoutingAsks.following(engine, (RoutingAsks.CarriedRequest) request)),
    LAID(RoutingAsks.CarriedRequest.class, (engine, request) -> RoutingAsks.laid(engine, (RoutingAsks.CarriedRequest) request)),
    JOGS(RoutingAsks.JogsRequest.class, (engine, request) -> RoutingAsks.jogs(engine, (RoutingAsks.JogsRequest) request)),
    CORNERS(RoutingAsks.CornersRequest.class, (engine, request) -> RoutingAsks.corners(engine, (RoutingAsks.CornersRequest) request)),
    NODES(RoutingAsks.NodesRequest.class, (engine, request) -> RoutingAsks.nodes(engine, (RoutingAsks.NodesRequest) request)),
    TUNE(RoutingAsks.TuneRequest.class, (engine, request) -> RoutingAsks.tune(engine, (RoutingAsks.TuneRequest) request)),
    TUNE_TRACK(RoutingAsks.TuneTrackRequest.class, (engine, request) -> RoutingAsks.tuneTrack(engine, (RoutingAsks.TuneTrackRequest) request));

    private final Class<?>                                 request;
    private final BiFunction<RoutingEngine, Object, Object> answering;

    RoutingGesture(Class<?> request, BiFunction<RoutingEngine, Object, Object> answering) {
        this.request = request;
        this.answering = answering;
    }

    /** The type its request is read as. */
    public Class<?> request() {
        return request;
    }

    /** Its answer: the engine's call made of the request. */
    public Object answer(RoutingEngine engine, Object request) {
        return answering.apply(engine, request);
    }

    /** Its name as an editor writes it. */
    public String word() {
        return name().toLowerCase(Locale.ROOT).replace('_', '-');
    }

    /** The gesture of a name; empty for a name of none. */
    public static Optional<RoutingGesture> of(String word) {
        return Arrays.stream(values()).filter(gesture -> gesture.word().equals(word)).findFirst();
    }
}
