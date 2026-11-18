package org.jmouse.cad.live;

import org.jmouse.cad.management.ExchangeController;
import org.jmouse.cad.management.RoutingController;
import org.jmouse.cad.pcb.protocol.RoutingAsks;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

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

        /**
         * The answer to a request as written, its layouts given read: the rest of it read as ever, the layouts set in it as they are —
         * never read again from their trees at every step.
         */
        Object answer(String board, JsonNode written, Map<String, Object> layouts, ObjectMapper mapper) {
            R read = mapper.treeToValue(written, request);
            return answering.answer(board, layouts.isEmpty() ? read : withLayouts(read, layouts));
        }

        /** The type a field of the request is read as, where the request has one of that name. */
        Optional<Class<?>> typeOf(String field) {
            return Arrays.stream(request.getRecordComponents()).filter(component -> component.getName().equals(field))
                    .<Class<?>>map(RecordComponent::getType).findFirst();
        }

        private R withLayouts(R read, Map<String, Object> layouts) {
            RecordComponent[] components = request.getRecordComponents();
            Object[] values = new Object[components.length];
            Class<?>[] types = new Class<?>[components.length];
            try {
                for (int index = 0; index < components.length; index++) {
                    RecordComponent component = components[index];
                    types[index] = component.getType();
                    values[index] = layouts.containsKey(component.getName()) ? layouts.get(component.getName()) : component.getAccessor().invoke(read);
                }
                return request.getDeclaredConstructor(types).newInstance(values);
            } catch (ReflectiveOperationException unmade) {
                throw new IllegalStateException("A " + request.getSimpleName() + " could not be made with its layouts", unmade);
            }
        }
    }

    private final Map<PcbAskKind, Ask<?>> asks = new EnumMap<>(PcbAskKind.class);
    private final ObjectMapper            mapper;

    PcbAsks(RoutingController routing, ExchangeController exchange, ObjectMapper mapper) {
        this.mapper = mapper;
        asks.put(PcbAskKind.OPEN, new Ask<>(RoutingController.OpenRequest.class, (board, request) -> routing.open(request)));
        asks.put(PcbAskKind.LEG, new Ask<>(RoutingAsks.LegRequest.class, routing::leg));
        asks.put(PcbAskKind.CLICKED_LEG, new Ask<>(RoutingAsks.LegRequest.class, routing::leg));
        asks.put(PcbAskKind.SEGMENT, new Ask<>(RoutingAsks.SegmentRequest.class, routing::segment));
        asks.put(PcbAskKind.CORNER, new Ask<>(RoutingAsks.CornerRequest.class, routing::corner));
        asks.put(PcbAskKind.FOLLOWING, new Ask<>(RoutingAsks.CarriedRequest.class, routing::following));
        asks.put(PcbAskKind.LAID, new Ask<>(RoutingAsks.CarriedRequest.class, routing::laid));
        asks.put(PcbAskKind.JOGS, new Ask<>(RoutingAsks.JogsRequest.class, routing::jogs));
        asks.put(PcbAskKind.CORNERS, new Ask<>(RoutingAsks.CornersRequest.class, routing::corners));
        asks.put(PcbAskKind.NODES, new Ask<>(RoutingAsks.NodesRequest.class, routing::nodes));
        asks.put(PcbAskKind.TUNE, new Ask<>(RoutingAsks.TuneRequest.class, routing::tune));
        asks.put(PcbAskKind.TUNE_TRACK, new Ask<>(RoutingAsks.TuneTrackRequest.class, routing::tuneTrack));
        asks.put(PcbAskKind.DERIVED, new Ask<>(ExchangeController.PcbDerivedRequest.class, (board, request) -> exchange.derived(request)));
    }

    /** The answer to an ask of a kind, its request as written and its layouts read. */
    Object answer(PcbAskKind kind, String board, JsonNode request, Map<String, Object> layouts) {
        return asks.get(kind).answer(board, request, layouts, mapper);
    }

    /** The type an ask of a kind reads a layout field as; empty where its request has no such field. */
    Optional<Class<?>> layoutType(PcbAskKind kind, String field) {
        return asks.get(kind).typeOf(field);
    }
}
