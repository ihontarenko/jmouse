package org.jmouse.cad.live;

import org.jmouse.cad.management.ExchangeController;
import org.jmouse.cad.management.RoutingController;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.EnumMap;
import java.util.Map;

/**
 * What each kind of ask of a pcb's session does: the REST route's own method, its request read from the ask as the route reads
 * its body. One table, so an ask over the socket and a request over HTTP can never answer differently.
 */
final class PcbAsks {

    /** A route's work: the board it is asked of — none for opening one —, and its request. */
    @FunctionalInterface
    private interface Answering<R> {
        Object answer(String board, R request);
    }

    private record Ask<R>(Class<R> request, Answering<R> answering) {

        Object answer(String board, JsonNode written, ObjectMapper mapper) {
            return answering.answer(board, mapper.treeToValue(written, request));
        }
    }

    private final Map<PcbAskKind, Ask<?>> asks = new EnumMap<>(PcbAskKind.class);
    private final ObjectMapper            mapper;

    PcbAsks(RoutingController routing, ExchangeController exchange, ObjectMapper mapper) {
        this.mapper = mapper;
        asks.put(PcbAskKind.OPEN, new Ask<>(RoutingController.OpenRequest.class, (board, request) -> routing.open(request)));
        asks.put(PcbAskKind.LEG, new Ask<>(RoutingController.LegRequest.class, routing::leg));
        asks.put(PcbAskKind.CLICKED_LEG, new Ask<>(RoutingController.LegRequest.class, routing::leg));
        asks.put(PcbAskKind.SEGMENT, new Ask<>(RoutingController.SegmentRequest.class, routing::segment));
        asks.put(PcbAskKind.CORNER, new Ask<>(RoutingController.CornerRequest.class, routing::corner));
        asks.put(PcbAskKind.FOLLOWING, new Ask<>(RoutingController.CarriedRequest.class, routing::following));
        asks.put(PcbAskKind.LAID, new Ask<>(RoutingController.CarriedRequest.class, routing::laid));
        asks.put(PcbAskKind.JOGS, new Ask<>(RoutingController.JogsRequest.class, routing::jogs));
        asks.put(PcbAskKind.CORNERS, new Ask<>(RoutingController.CornersRequest.class, routing::corners));
        asks.put(PcbAskKind.NODES, new Ask<>(RoutingController.NodesRequest.class, routing::nodes));
        asks.put(PcbAskKind.TUNE, new Ask<>(RoutingController.TuneRequest.class, routing::tune));
        asks.put(PcbAskKind.TUNE_TRACK, new Ask<>(RoutingController.TuneTrackRequest.class, routing::tuneTrack));
        asks.put(PcbAskKind.DERIVED, new Ask<>(ExchangeController.PcbDerivedRequest.class, (board, request) -> exchange.derived(request)));
    }

    /** The answer to an ask of a kind, its request as written. */
    Object answer(PcbAskKind kind, String board, JsonNode request) {
        return asks.get(kind).answer(board, request, mapper);
    }
}
