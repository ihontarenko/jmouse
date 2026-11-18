package org.jmouse.cad.pcb.checks;

import org.jmouse.cad.pcb.drawing.FootprintDrawing;
import org.jmouse.cad.pcb.drawing.PcbDrawing;
import org.jmouse.cad.pcb.geometry.CopperNets;
import org.jmouse.cad.pcb.geometry.CopperPieces;
import org.jmouse.cad.pcb.geometry.PlacedDesignator;
import org.jmouse.cad.pcb.geometry.Teardrops;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A pcb's checks, as a pcb house would make them before it makes the pcb: copper of two nets too near on a layer, copper
 * joining nets the circuit keeps apart, tracks, vias and holes thinner or nearer than the pcb is made with, copper too
 * near the edge or beyond it, an edge that does not close, copper in a keepout, parts crowding each other, silkscreen
 * printed cut, track ends joined to nothing, pairs laid unevenly — and what is not laid yet. Each finding names what it is
 * about, to be selected, and where it is, to be marked.
 *
 * <p>The engine's one set of checks: every output of a pcb runs them first, and the editor shows what they find.</p>
 */
public final class PcbChecks {

    /**
     * The checks, in the order their findings are told: what the parts stand on, what the copper joins, the pairs; then what is
     * measured against the pcb's values — gaps, sizes, holes, the edge —; then the edge's closing, keepouts, zones, courtyards,
     * silkscreen, loose ends, and what is not laid yet.
     */
    private static final List<PcbCheck> CHECKS = List.of(new FootprintCheck(), new ShortCheck(), new PairCheck(),
            PcbCheck.ruled(new GapCheck()), PcbCheck.ruled(new SizeCheck()), PcbCheck.ruled(new HoleCheck()), PcbCheck.ruled(new EdgeCheck()),
            new OpenEdgeCheck(), new KeepoutCheck(), new ZoneCheck(), new CourtyardCheck(), new SilkscreenCheck(), new LooseEndCheck(),
            new UnlaidCheck());

    private PcbChecks() {
    }

    /**
     * What the checks read.
     *
     * @param footprints each placed part's footprint, by designator
     * @param named      for each placement, the footprint its part names; empty where it names none
     * @param partNets   each part's pads and the net each is on, by designator, then pad number, as the circuit wires them
     * @param values     the values the pcb is held to, by rule word; empty while they are not known, when only what needs no
     *                   rule is checked
     */
    public record Subject(PcbDrawing pcb, Map<String, FootprintDrawing> footprints, Map<String, Optional<String>> named,
                          Map<String, Map<String, String>> partNets, Optional<Map<String, Double>> values) {
    }

    /**
     * How the checks measure.
     *
     * @param tolerance  how much nearer than a rule a value may be and still pass: what a millimetre written to a few places
     *                   loses
     * @param namedGaps  how many narrow gaps are named one by one; beyond them, the rest are counted
     * @param namedMeetings how many places a short's nets meet at are named in its finding
     * @param curveSteps how many straight pieces a curve is followed in
     * @param designator how a part's designator is set and how wide its letters run
     * @param teardrops  how the drops its tracks are widened with are shaped: the copper the checks measure is the copper made
     */
    public record Settings(double tolerance, int namedGaps, int namedMeetings, int curveSteps, PlacedDesignator.Rules designator,
                           Teardrops.Shape teardrops) {
    }

    /** Everything the checks find on a pcb, its copper worked out as given. */
    public static List<PcbFinding> of(Subject subject, CopperNets nets, Settings settings) {
        List<Teardrops.Drop> drops = Teardrops.of(subject.pcb(), nets.pads(), settings.teardrops());
        CheckContext context = new CheckContext(subject, settings, nets, CopperPieces.of(subject.pcb(), nets, drops, settings.curveSteps()));
        return CHECKS.stream().flatMap(check -> check.findings(context).stream()).toList();
    }
}
