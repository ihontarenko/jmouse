package org.jmouse.cad.management;

import org.jmouse.access.enforcement.PublicEndpoint;
import org.jmouse.cad.pcb.drawing.PcbDrawing;
import org.jmouse.cad.pcb.drawing.PcbDrawing.Point;
import org.jmouse.cad.pcb.geometry.CopperNets;
import org.jmouse.cad.pcb.routing.Autorouter;
import org.jmouse.cad.pcb.routing.LengthTuning;
import org.jmouse.cad.pcb.routing.RoutingEngine;
import org.jmouse.cad.pcb.routing.Tightening;
import org.jmouse.cad.pcb.routing.TrackRouter;
import org.jmouse.cad.store.catalogue.WrittenOwners;
import org.jmouse.cad.store.editing.LanguageEditing;
import org.jmouse.cad.store.exchange.RoutingBoards;
import org.jmouse.cad.store.problem.CadProblem;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

    private static TrackRouter.RoutingMode modeOf(String word) {
        return TrackRouter.RoutingMode.valueOf(word.toUpperCase(Locale.ROOT));
    }

    private static Tightening.JogHandling jogsOf(String word) {
        return Tightening.JogHandling.valueOf(word.toUpperCase(Locale.ROOT));
    }

    /** How the reader lays copper now: {@code walkaround} or {@code shove}, the angle, {@code kept} or {@code merged} jogs. */
    public record StyleView(String mode, double angleStep, String jogs) {

        TrackRouter.LayingStyle style() {
            return new TrackRouter.LayingStyle(modeOf(mode), angleStep, jogsOf(jogs));
        }
    }

    /** The pcb's tracks as a gesture leaves them; null where it could not move at all. */
    public record TracksView(List<PcbDrawing.Track> tracks) {
    }

    /** A track being laid toward the pointer, and the corners the reader lays tracks with. */
    public record LegRequest(PcbDrawing drawing, String layer, String net, double width, Point from, Point to, double angleStep, String mode,
                             double rounding) {
    }

    /**
     * Where it goes after its last corner, every track as a shove leaves them — null: untouched —, whether it got there, and the
     * corners it is drawn with: the reader's, as far as its gaps let them.
     */
    public record LegView(List<Point> points, List<PcbDrawing.Track> tracks, boolean reached, double rounding) {
    }

    @PostMapping("/pcb/routing/{board}/leg")
    @PublicEndpoint("a board the caller opened from a text it sent")
    public LegView leg(@PathVariable String board, @RequestBody LegRequest request) {
        RoutingEngine.LaidLeg laid = boards.engineOf(board).leg(request.drawing(), request.layer(), Optional.ofNullable(request.net()), request.width(),
                request.from(), request.to(), request.angleStep(), modeOf(request.mode()), request.rounding());
        TrackRouter.RoutedLeg leg = laid.leg();
        return new LegView(leg.points(), leg.tracks().orElse(null), leg.reached(), laid.rounding());
    }

    /** A track's stretch carried across, from where it was pressed. */
    public record SegmentRequest(PcbDrawing drawing, int track, int segment, double dx, double dy, StyleView style, Point pressed) {
    }

    @PostMapping("/pcb/routing/{board}/segment")
    @PublicEndpoint("a board the caller opened from a text it sent")
    public TracksView segment(@PathVariable String board, @RequestBody SegmentRequest request) {
        return new TracksView(boards.engineOf(board).segmentCarried(request.drawing(), request.track(), request.segment(), request.dx(), request.dy(),
                request.style().style(), Optional.ofNullable(request.pressed())).map(PcbDrawing::tracks).orElse(null));
    }

    /** A track's corner carried to a point. */
    public record CornerRequest(PcbDrawing drawing, int track, int corner, Point to, StyleView style) {
    }

    @PostMapping("/pcb/routing/{board}/corner")
    @PublicEndpoint("a board the caller opened from a text it sent")
    public TracksView corner(@PathVariable String board, @RequestBody CornerRequest request) {
        return new TracksView(boards.engineOf(board).cornerCarried(request.drawing(), request.track(), request.corner(), request.to(),
                request.style().style()).tracks());
    }

    /** A part carried: the layout it began on, the layout it is carried to, the angle tracks are laid at. */
    public record CarriedRequest(PcbDrawing before, PcbDrawing moved, double angleStep) {
    }

    @PostMapping("/pcb/routing/{board}/following")
    @PublicEndpoint("a board the caller opened from a text it sent")
    public TracksView following(@PathVariable String board, @RequestBody CarriedRequest request) {
        return new TracksView(boards.engineOf(board).tracksFollowing(request.before(), request.moved(), request.angleStep()).tracks());
    }

    @PostMapping("/pcb/routing/{board}/laid")
    @PublicEndpoint("a board the caller opened from a text it sent")
    public TracksView laid(@PathVariable String board, @RequestBody CarriedRequest request) {
        return new TracksView(boards.engineOf(board).tracksLaid(request.before(), request.moved(), request.angleStep()).tracks());
    }

    /** Tracks whose jogs are merged, by their places; none chosen, every one. */
    public record JogsRequest(PcbDrawing drawing, List<Integer> chosen) {
    }

    /** Tracks whose corners are rounded so, by their places; none chosen, every one. */
    public record CornersRequest(PcbDrawing drawing, double rounding, List<Integer> chosen) {
    }

    private static List<Integer> chosenOf(List<Integer> chosen) {
        return chosen == null ? List.of() : chosen;
    }

    @PostMapping("/pcb/routing/{board}/jogs")
    @PublicEndpoint("a board the caller opened from a text it sent")
    public TracksView jogs(@PathVariable String board, @RequestBody JogsRequest request) {
        return new TracksView(boards.engineOf(board).withoutJogs(request.drawing(), chosenOf(request.chosen())).tracks());
    }

    @PostMapping("/pcb/routing/{board}/corners")
    @PublicEndpoint("a board the caller opened from a text it sent")
    public TracksView corners(@PathVariable String board, @RequestBody CornersRequest request) {
        return new TracksView(boards.engineOf(board).withCorners(request.drawing(), request.rounding(), chosenOf(request.chosen())).tracks());
    }

    /** A track tuned where it is pointed at: on a layer, how far off its edge the point may be, the length — null for its pair partner's. */
    public record TuneRequest(PcbDrawing drawing, String layer, Point point, double reach, Double length) {
    }

    /** A chosen track tuned wherever a meander fits on it: the length — null for its pair partner's. */
    public record TuneTrackRequest(PcbDrawing drawing, int track, Double length) {
    }

    /** A track bent to length — which, its points, what was added, its net and the lengths it went between —, or why not: one of them. */
    public record TuningView(Integer track, List<Point> points, Double added, String net, Double from, Double to, String refusal) {

        static TuningView of(LengthTuning.Tuning tuning) {
            return switch (tuning) {
                case LengthTuning.Tuned tuned -> new TuningView(tuned.track(), tuned.points(), tuned.added(), tuned.net().orElse(null), tuned.from(),
                        tuned.to(), null);
                case LengthTuning.Refused refused -> new TuningView(null, null, null, null, null, null, refused.refusal());
            };
        }
    }

    private static java.util.OptionalDouble lengthOf(Double length) {
        return length == null ? java.util.OptionalDouble.empty() : java.util.OptionalDouble.of(length);
    }

    @PostMapping("/pcb/routing/{board}/tune")
    @PublicEndpoint("a board the caller opened from a text it sent")
    public TuningView tune(@PathVariable String board, @RequestBody TuneRequest request) {
        return TuningView.of(boards.engineOf(board).tunedAt(request.drawing(), request.layer(), request.point(), request.reach(), lengthOf(request.length())));
    }

    @PostMapping("/pcb/routing/{board}/tune-track")
    @PublicEndpoint("a board the caller opened from a text it sent")
    public TuningView tuneTrack(@PathVariable String board, @RequestBody TuneTrackRequest request) {
        return TuningView.of(boards.engineOf(board).tunedAnywhere(request.drawing(), request.track(), lengthOf(request.length())));
    }

    /** A whole pcb routed, by a name the caller gives the run so it can stop it: its layout, the angle and corners tracks are laid with, {@code vias} or {@code links}, and the nets; none, all. */
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
