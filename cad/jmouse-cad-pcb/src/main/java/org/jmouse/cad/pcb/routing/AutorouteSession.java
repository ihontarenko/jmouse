package org.jmouse.cad.pcb.routing;

import org.jmouse.cad.Point;
import org.jmouse.cad.pcb.drawing.PcbDrawing;
import org.jmouse.cad.pcb.drawing.Track;
import org.jmouse.cad.pcb.geometry.CopperNets;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * One routing as it runs: what was asked and by when, the layouts it has worked out, and how far it has come — what every part of the
 * autorouter reads and tells the reader through.
 */
final class AutorouteSession {

    private final RoutingBoard                  board;
    private final Autorouter.Request            request;
    private final RoutingSettings.Autorouting   settings;
    private final Map<PcbDrawing, RoutingWorld> worlds = new IdentityHashMap<>();
    private final long                          until;
    private int                                 tried;
    private int                                 total;
    private int                                 laidBefore;
    private int                                 round;
    private int                                 laidNow;
    private List<CopperNets.Unlaid>             refusedNow = List.of();

    AutorouteSession(RoutingBoard board, Autorouter.Request request) {
        this.board = board;
        this.request = request;
        this.settings = board.settings().autorouting();
        this.until = System.nanoTime() + (long) (settings.budgetSeconds() * 1e9);
    }

    RoutingBoard board() {
        return board;
    }

    Autorouter.Request request() {
        return request;
    }

    RoutingSettings.Autorouting settings() {
        return settings;
    }

    /** A layout worked out, kept while the routing comes back to it; let go once more are kept than the settings allow. */
    RoutingWorld worldOf(PcbDrawing pcb) {
        if (worlds.size() > settings.keptLayouts()) {
            worlds.clear();
        }
        return worlds.computeIfAbsent(pcb, layout -> new RoutingWorld(layout, board));
    }

    /** The connections of a layout still to lay that the routing was asked for. */
    List<CopperNets.Unlaid> left(PcbDrawing pcb) {
        return worldOf(pcb).nets().unlaid().stream()
                .filter(line -> request.onlyNets().isEmpty() || request.onlyNets().contains(line.net())).toList();
    }

    /** Whether the routing is to stop: asked to, or out of its time. */
    boolean outOfTime() {
        return request.stopped().getAsBoolean() || System.nanoTime() > until;
    }

    /** The directions its tracks may take: 90 where the reader lays at right angles, the settings' own otherwise. */
    double angleStep() {
        return request.angleStep() == 90 ? 90 : settings.angleStep();
    }

    /** A point written to the settings' places: a step's arithmetic leaves no more than that worth saying. */
    Point written(Point point) {
        return new Point(Precision.writtenTo(point.x(), settings.decimals()), Precision.writtenTo(point.y(), settings.decimals()));
    }

    /** How many connections have been tried. */
    int tried() {
        return tried;
    }

    /** One more connection tried. */
    void triedOne() {
        tried += 1;
    }

    /** How many connections there are to lay, the routing begun. */
    void totalOf(int connections) {
        total = connections;
    }

    /** The round it is in, from 1. */
    void inRound(int number) {
        round = number;
    }

    /** The most connections a finished round laid, said while the next one has laid fewer. */
    void bestLaid(int laid) {
        laidBefore = laid;
    }

    /** How a round stands: so many laid, these refused. */
    void counted(int laid, List<CopperNets.Unlaid> refused) {
        laidNow = laid;
        refusedNow = List.copyOf(refused);
    }

    /** The reader told what was just done. */
    void tell(Autorouter.Step step, PcbDrawing pcb, Optional<CopperNets.Unlaid> line, List<Track> attempt, Optional<String> reason) {
        request.moments().accept(new Autorouter.Moment(step, pcb, line, attempt, reason, refusedNow, Math.max(laidNow, laidBefore), tried, total,
                round));
    }

    void tell(Autorouter.Step step, PcbDrawing pcb, Optional<CopperNets.Unlaid> line) {
        tell(step, pcb, line, List.of(), Optional.empty());
    }
}
