package org.jmouse.cad.management;

import org.jmouse.access.enforcement.PublicEndpoint;
import org.jmouse.cad.pcb.drawing.PcbDrawing;
import org.jmouse.cad.pcb.geometry.CopperNets;
import org.jmouse.cad.pcb.routing.Autorouter;
import org.jmouse.cad.pcb.routing.RoutingEngine;
import org.jmouse.cad.routing.protocol.RoutingAsks;
import org.jmouse.cad.routing.protocol.RoutingAsks.CarriedRequest;
import org.jmouse.cad.routing.protocol.RoutingAsks.CornerRequest;
import org.jmouse.cad.routing.protocol.RoutingAsks.CornersRequest;
import org.jmouse.cad.routing.protocol.RoutingAsks.JogsRequest;
import org.jmouse.cad.routing.protocol.RoutingAsks.LegRequest;
import org.jmouse.cad.routing.protocol.RoutingAsks.LegView;
import org.jmouse.cad.routing.protocol.RoutingAsks.NodesRequest;
import org.jmouse.cad.routing.protocol.RoutingAsks.NodesView;
import org.jmouse.cad.routing.protocol.RoutingAsks.SegmentRequest;
import org.jmouse.cad.routing.protocol.RoutingAsks.TracksView;
import org.jmouse.cad.routing.protocol.RoutingAsks.TuneRequest;
import org.jmouse.cad.routing.protocol.RoutingAsks.TuneTrackRequest;
import org.jmouse.cad.routing.protocol.RoutingAsks.TuningView;
import org.jmouse.cad.routing.protocol.RoutingBoardJson;
import org.jmouse.cad.routing.protocol.RoutingRefused;
import org.jmouse.cad.store.catalogue.WrittenOwners;
import org.jmouse.cad.store.editing.LanguageEditing;
import org.jmouse.cad.store.exchange.RoutingBoards;
import org.jmouse.cad.store.problem.CadProblem;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 🛤 The router an editor lays copper with — {@code JMOUSE_CAD_API.md} §2.7. A board is opened for a pcb's text once; then
 * every gesture names it and sends the layout it is asked of, and is answered with the tracks as they come out: a track laid
 * toward the pointer, a stretch or a corner carried, a part's tracks following it, jogs merged, corners rounded. A whole pcb is
 * routed as a stream of moments, one JSON line each, the last its answer.
 */
@RestController
@RequestMapping(CadManagementRoutes.PREFIX)
public class RoutingController {

    private static final MediaType LINES = MediaType.parseMediaType("application/x-ndjson");

    private final RoutingBoards  boards;
    private final LanguageEditing editing;
    private final CadJsonAnswers  json;
    /** The routings running, by the name each caller gave its run: what a stop is asked by. */
    private final Map<String, AtomicBoolean> runs = new ConcurrentHashMap<>();

    RoutingController(RoutingBoards boards, LanguageEditing editing, CadJsonAnswers json) {
        this.boards = boards;
        this.editing = editing;
        this.json = json;
    }

    /** A pcb a board is opened for: its text and layout as {@code /pcb/derived} takes them. */
    public record OpenRequest(String source, PcbDrawing drawing, String owner) {
    }

    /** An open board, by the name every gesture asks it by. */
    public record OpenBoard(String board) {
    }

    @PostMapping("/pcb/routing")
    @PublicEndpoint("a text the caller sent; the components and footprints it names are read as the reader may see them")
    public OpenBoard open(@RequestBody OpenRequest request) {
        String source = request.drawing() == null ? request.source() : editing.pcb(request.source(), request.drawing(), null, null).source();
        return new OpenBoard(boards.open(source, WrittenOwners.optional(request.owner())));
    }

    /**
     * The board an open board was worked out of — footprints, nets, values, checks, settings —, as JSON: what a page needs to route
     * on it itself, without the store the text was read from. Written by the protocol's own mapper, which a page reads it with.
     */
    @GetMapping(value = "/pcb/routing/{board}/board", produces = MediaType.APPLICATION_JSON_VALUE)
    @PublicEndpoint("a board the caller opened from a text it sent")
    public String board(@PathVariable String board) {
        return RoutingBoardJson.write(boards.engineOf(board).board());
    }

    @PostMapping("/pcb/routing/{board}/leg")
    @PublicEndpoint("a board the caller opened from a text it sent")
    public LegView leg(@PathVariable String board, @RequestBody LegRequest request) {
        return RoutingAsks.leg(boards.engineOf(board), request);
    }

    @PostMapping("/pcb/routing/{board}/segment")
    @PublicEndpoint("a board the caller opened from a text it sent")
    public TracksView segment(@PathVariable String board, @RequestBody SegmentRequest request) {
        return RoutingAsks.segment(boards.engineOf(board), request);
    }

    @PostMapping("/pcb/routing/{board}/corner")
    @PublicEndpoint("a board the caller opened from a text it sent")
    public TracksView corner(@PathVariable String board, @RequestBody CornerRequest request) {
        return RoutingAsks.corner(boards.engineOf(board), request);
    }

    @PostMapping("/pcb/routing/{board}/following")
    @PublicEndpoint("a board the caller opened from a text it sent")
    public TracksView following(@PathVariable String board, @RequestBody CarriedRequest request) {
        return RoutingAsks.following(boards.engineOf(board), request);
    }

    @PostMapping("/pcb/routing/{board}/laid")
    @PublicEndpoint("a board the caller opened from a text it sent")
    public TracksView laid(@PathVariable String board, @RequestBody CarriedRequest request) {
        try {
            return RoutingAsks.laid(boards.engineOf(board), request);
        } catch (RoutingRefused refused) {
            throw CadProblem.refused(refused.getMessage());
        }
    }

    @PostMapping("/pcb/routing/{board}/jogs")
    @PublicEndpoint("a board the caller opened from a text it sent")
    public TracksView jogs(@PathVariable String board, @RequestBody JogsRequest request) {
        return RoutingAsks.jogs(boards.engineOf(board), request);
    }

    @PostMapping("/pcb/routing/{board}/corners")
    @PublicEndpoint("a board the caller opened from a text it sent")
    public TracksView corners(@PathVariable String board, @RequestBody CornersRequest request) {
        return RoutingAsks.corners(boards.engineOf(board), request);
    }

    @PostMapping("/pcb/routing/{board}/nodes")
    @PublicEndpoint("a board the caller opened from a text it sent")
    public NodesView nodes(@PathVariable String board, @RequestBody NodesRequest request) {
        return RoutingAsks.nodes(boards.engineOf(board), request);
    }

    @PostMapping("/pcb/routing/{board}/tune")
    @PublicEndpoint("a board the caller opened from a text it sent")
    public TuningView tune(@PathVariable String board, @RequestBody TuneRequest request) {
        return RoutingAsks.tune(boards.engineOf(board), request);
    }

    @PostMapping("/pcb/routing/{board}/tune-track")
    @PublicEndpoint("a board the caller opened from a text it sent")
    public TuningView tuneTrack(@PathVariable String board, @RequestBody TuneTrackRequest request) {
        return RoutingAsks.tuneTrack(boards.engineOf(board), request);
    }

    public record AutorouteRequest(String run, PcbDrawing drawing, double angleStep, double rounding, String crossing, List<String> onlyNets) {
    }

    /** What a routing laid beyond the layout it was asked of: the tracks, vias and links after the layout's own. */
    public record AddedView(List<PcbDrawing.Track> tracks, List<PcbDrawing.Via> vias, List<PcbDrawing.Placement> placements) {

        static AddedView of(PcbDrawing start, PcbDrawing now) {
            return new AddedView(now.tracks().subList(start.tracks().size(), now.tracks().size()),
                    now.vias().subList(start.vias().size(), now.vias().size()),
                    now.placements().subList(start.placements().size(), now.placements().size()));
        }
    }

    /** A moment of a routing, as a line of the stream. */
    public record MomentView(String step, CopperNets.Unlaid line, List<PcbDrawing.Track> attempt, String reason, List<CopperNets.Unlaid> refused,
                             int laid, int tried, int total, int round, AddedView added) {
    }

    /** The routing's answer, the stream's last line. */
    public record RoutedView(int laid, List<CopperNets.Unlaid> unrouted, boolean stopped, AddedView added) {
    }

    /** One line of the stream: a moment, or the answer. */
    public record StreamLine(MomentView moment, RoutedView done) {
    }

    @PostMapping("/pcb/routing/{board}/autoroute")
    @PublicEndpoint("a board the caller opened from a text it sent")
    public ResponseEntity<ResponseBodyEmitter> autoroute(@PathVariable String board, @RequestBody AutorouteRequest request) {
        RoutingEngine engine = boards.engineOf(board);
        // The routing ends itself, by its own budget or by the reader stopping it: nothing else may cut it short.
        ResponseBodyEmitter emitter = new ResponseBodyEmitter(0L);
        AtomicBoolean stopped = new AtomicBoolean();
        AtomicBoolean stopAsked = new AtomicBoolean();
        PcbDrawing start = request.drawing();
        if (request.run() != null && runs.putIfAbsent(request.run(), stopAsked) != null) {
            throw CadProblem.conflict("A routing named " + request.run() + " is running already");
        }
        Thread.ofVirtual().name("autoroute-" + board.substring(0, 8)).start(() -> {
            try {
                Autorouter.Request asked = new Autorouter.Request(start, request.angleStep(), request.rounding(),
                        Autorouter.Crossing.valueOf(request.crossing().toUpperCase(Locale.ROOT)),
                        request.onlyNets() == null ? Set.of() : Set.copyOf(request.onlyNets()), () -> stopped.get() || stopAsked.get(),
                        moment -> send(emitter, stopped, new StreamLine(new MomentView(moment.step().name().toLowerCase(Locale.ROOT).replace('_', '-'),
                                moment.line().orElse(null), moment.attempt(), moment.reason().orElse(null), moment.refused(), moment.laid(), moment.tried(),
                                moment.total(), moment.round(), AddedView.of(start, moment.pcb())), null)));
                Autorouter.Routed routed = Autorouter.route(engine.board(), asked);
                send(emitter, stopped, new StreamLine(null, new RoutedView(routed.laid(), routed.unrouted(), routed.stopped(),
                        AddedView.of(start, routed.pcb()))));
                emitter.complete();
            } catch (RuntimeException failure) {
                emitter.completeWithError(failure);
            } finally {
                if (request.run() != null) {
                    runs.remove(request.run());
                }
            }
        });
        return ResponseEntity.ok().contentType(LINES).body(emitter);
    }

    /** A running routing asked to stop: it answers what it laid so far as its stream's last line. Nothing running so named, nothing to stop. */
    @PostMapping("/pcb/routing/{board}/autoroute/{run}/stop")
    @PublicEndpoint("a routing the caller started and named")
    public void stop(@PathVariable String board, @PathVariable String run) {
        Optional.ofNullable(runs.get(run)).ifPresent(asked -> asked.set(true));
    }

    /** A line sent; a reader gone — it stopped the routing — stops it here too, and nothing more is sent. */
    private void send(ResponseBodyEmitter emitter, AtomicBoolean stopped, StreamLine line) {
        if (stopped.get()) {
            return;
        }
        try {
            emitter.send(json.written(line) + "\n", MediaType.TEXT_PLAIN);
        } catch (IOException | IllegalStateException gone) {
            stopped.set(true);
        }
    }
}
