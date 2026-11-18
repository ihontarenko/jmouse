package org.jmouse.cad.pcb.checks;

import org.jmouse.cad.Point;
import org.jmouse.cad.pcb.drawing.NetClass;
import org.jmouse.cad.pcb.drawing.Pair;
import org.jmouse.cad.pcb.drawing.Placement;
import org.jmouse.cad.pcb.drawing.Track;
import org.jmouse.cad.pcb.drawing.Via;
import org.jmouse.cad.pcb.drawing.Zone;
import org.jmouse.cad.pcb.drawing.Keepout;
import org.jmouse.cad.pcb.drawing.Lettering;
import org.jmouse.geometry.index.BoxIndex;
import org.jmouse.geometry.index.BoxTree;

import org.jmouse.geometry.index.Box;

import org.jmouse.cad.pcb.PcbVocabulary;
import org.jmouse.cad.pcb.drawing.FootprintDrawing;
import org.jmouse.cad.pcb.drawing.NetRules;
import org.jmouse.cad.pcb.drawing.PcbDrawing;
import org.jmouse.cad.pcb.geometry.CopperNets;
import org.jmouse.cad.pcb.geometry.CopperPieces;
import org.jmouse.cad.pcb.geometry.CopperShape;
import org.jmouse.cad.pcb.geometry.CopperShapes;
import org.jmouse.cad.pcb.geometry.NetLengths;
import org.jmouse.cad.pcb.geometry.PathOutlines;
import org.jmouse.cad.pcb.geometry.PlacedDesignator;
import org.jmouse.cad.pcb.geometry.Teardrops;
import org.jmouse.svg.path.node.PathPolylines;
import org.jmouse.svg.path.parser.PathParser;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.jmouse.cad.document.FindingSeverity.ERROR;
import static org.jmouse.cad.document.FindingSeverity.NOTICE;
import static org.jmouse.cad.document.FindingSeverity.WARNING;
import static org.jmouse.cad.pcb.geometry.CopperPieces.key;

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

    private static final PathParser PATHS = new PathParser();

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

    private final Subject                   subject;
    private final Settings                  settings;
    private final PcbDrawing                pcb;
    private final CopperNets                nets;
    private final List<CopperPieces.Piece>  pieces;
    /** The pieces by their boxes: what stands near a piece found among its neighbours, never every piece of the pcb. */
    private final BoxIndex                  piecesNear;
    /** Each piece by its kind and its place among its kind's: a member of the joining found at once. */
    private final Map<CopperPieces.Kind, Map<Integer, CopperPieces.Piece>> piecesByKind = new EnumMap<>(CopperPieces.Kind.class);

    private PcbChecks(Subject subject, Settings settings, CopperNets nets, List<CopperPieces.Piece> pieces) {
        this.subject = subject;
        this.settings = settings;
        this.pcb = subject.pcb();
        this.nets = nets;
        this.pieces = pieces;
        this.piecesNear = BoxTree.of(pieces.stream().map(piece -> piece.shape().box()).toList());
        pieces.forEach(piece -> piecesByKind.computeIfAbsent(piece.kind(), kind -> new HashMap<>()).putIfAbsent(piece.index(), piece));
    }

    /** Everything the checks find on a pcb, its copper worked out as given. */
    public static List<PcbFinding> of(Subject subject, CopperNets nets, Settings settings) {
        List<Teardrops.Drop> drops = Teardrops.of(subject.pcb(), nets.pads(), settings.teardrops());
        return new PcbChecks(subject, settings, nets, CopperPieces.of(subject.pcb(), nets, drops, settings.curveSteps())).findings();
    }

    /**
     * A short as the reader is told it: the nets it joins and where — for each two, the chain of copper joining them, its pieces
     * the finding's keys, the first few chains named, the middle of the first chain its point —, never every piece the joined
     * copper spreads to.
     */
    private PcbFinding shortFinding(CopperNets.Short joined) {
        String joins = "Copper joins " + String.join(" and ", joined.nets()) + ", which the circuit keeps apart";
        Set<String> keys = new LinkedHashSet<>();
        List<String> named = new ArrayList<>();
        Optional<Point> at = Optional.empty();
        for (CopperNets.Joined join : joined.joins()) {
            join.chain().forEach(member -> keys.add(keyOf(member)));
            List<CopperNets.Member> chain = join.chain();
            if (named.size() < settings.namedMeetings()) {
                List<String> through = chain.subList(1, chain.size() - 1).stream().map(member -> labelOf(member, pieceOf(member))).toList();
                named.add(labelOf(chain.getFirst(), pieceOf(chain.getFirst())) + " of " + join.firstNet() + " reaches "
                          + labelOf(chain.getLast(), pieceOf(chain.getLast())) + " of " + join.secondNet()
                          + (through.isEmpty() ? " directly" : " through " + String.join(", ", through)));
            }
            if (at.isEmpty()) {
                at = touchIn(chain);
            }
        }
        int unnamed = joined.joins().size() - named.size();
        String where = named.isEmpty() ? "" : ": " + String.join("; ", named) + (unnamed > 0 ? "; and " + unnamed + " more" : "");
        return new PcbFinding(ERROR, joins + where, List.copyOf(keys), at);
    }

    /** Where two pieces in the middle of a chain touch: the place a short is shown at. */
    private Optional<Point> touchIn(List<CopperNets.Member> chain) {
        int middle = Math.max(0, chain.size() / 2 - 1);
        Optional<CopperPieces.Piece> first = pieceOf(chain.get(middle));
        Optional<CopperPieces.Piece> second = pieceOf(chain.get(Math.min(chain.size() - 1, middle + 1)));
        return first.isPresent() && second.isPresent() ? Optional.of(first.get().shape().gapTo(second.get().shape()).at()) : Optional.empty();
    }

    /** The piece of copper a member of the joining is, where the checks measure one — a zone's pour is not among them. */
    private Optional<CopperPieces.Piece> pieceOf(CopperNets.Member member) {
        if (member.kind() == CopperNets.MemberKind.ZONE) {
            return Optional.empty();
        }
        CopperPieces.Kind kind = CopperPieces.Kind.valueOf(member.kind().name());
        return Optional.ofNullable(piecesByKind.getOrDefault(kind, Map.of()).get(member.index()));
    }

    private String keyOf(CopperNets.Member member) {
        return member.kind() == CopperNets.MemberKind.ZONE ? key("zone", member.index())
                : pieceOf(member).map(CopperPieces.Piece::key).orElse(key(member.kind().name().toLowerCase(Locale.ROOT), member.index()));
    }

    private static String labelOf(CopperNets.Member member, Optional<CopperPieces.Piece> piece) {
        return piece.map(CopperPieces.Piece::label).orElse("Zone " + (member.index() + 1));
    }

    private List<PcbFinding> findings() {
        List<PcbFinding> findings = new ArrayList<>(footprintFindings());
        for (CopperNets.Short joined : nets.shorts()) {
            findings.add(shortFinding(joined));
        }
        findings.addAll(pairFindings());
        subject.values().ifPresent(values -> {
            findings.addAll(gapFindings(values));
            findings.addAll(sizeFindings(values));
            findings.addAll(holeFindings(values));
            findings.addAll(edgeFindings(values));
        });
        findings.addAll(openEdgeFindings());
        findings.addAll(keepoutFindings());
        findings.addAll(zoneFindings());
        findings.addAll(courtyardFindings());
        findings.addAll(silkscreenFindings());
        findings.addAll(looseEndFindings());
        int unlaid = nets.unlaid().size();
        if (unlaid > 0) {
            findings.add(new PcbFinding(NOTICE, unlaid + (unlaid == 1 ? " connection is" : " connections are") + " not laid yet", List.of()));
        }
        return findings;
    }

    // ─── what the parts stand on ───────────────────────────────────────────────────────────────────────

    /** Parts with no footprint chosen, and footprints the library does not have. */
    private List<PcbFinding> footprintFindings() {
        List<PcbFinding> findings = new ArrayList<>();
        List<Integer> bare = new ArrayList<>();
        List<Integer> missing = new ArrayList<>();
        for (int index = 0; index < pcb.placements().size(); index++) {
            String designator = pcb.placements().get(index).designator();
            Optional<String> named = subject.named().getOrDefault(designator, Optional.empty());
            if (named.isEmpty()) {
                bare.add(index);
            } else if (!subject.footprints().containsKey(designator)) {
                missing.add(index);
            }
        }
        if (!bare.isEmpty()) {
            String shown = bare.stream().limit(6).map(this::printed).collect(Collectors.joining(", "));
            findings.add(new PcbFinding(ERROR, shown + (bare.size() > 6 ? " and " + (bare.size() - 6) + " more" : "")
                    + (bare.size() == 1 ? " has" : " have") + " no footprint: choose one", bare.stream().map(index -> key("placement", index)).toList()));
        }
        if (!missing.isEmpty()) {
            findings.add(new PcbFinding(WARNING, "The footprint of " + missing.stream().map(this::printed).collect(Collectors.joining(", "))
                    + " is not in the library", missing.stream().map(index -> key("placement", index)).toList()));
        }
        return findings;
    }

    // ─── copper ────────────────────────────────────────────────────────────────────────────────────────

    /** Copper of two nets nearer on a layer than either net's clearance — or touching. */
    private List<PcbFinding> gapFindings(Map<String, Double> values) {
        NetRules rules = new NetRules(pcb, values);
        List<PcbFinding> findings = new ArrayList<>();
        Set<Long> compared = new HashSet<>();
        int unnamed = 0;
        double widest = Stream.concat(Stream.of(rule(values, PcbVocabulary.RULE_CLEARANCE)), pcb.netClasses().stream().map(NetClass::clearance))
                .mapToDouble(Double::doubleValue).max().orElse(0);
        for (String layer : pcb.copperLayerIds()) {
            List<Integer> onLayer = new ArrayList<>();
            for (int index = 0; index < pieces.size(); index++) {
                if (pieces.get(index).onLayer().test(layer)) {
                    onLayer.add(index);
                }
            }
            // Each piece measured against those whose boxes come within the widest gap of its own — never every other on the layer.
            BoxIndex onLayerNear = BoxTree.of(onLayer.stream().map(index -> pieces.get(index).shape().box()).toList());
            for (int first = 0; first < onLayer.size(); first++) {
                for (int second : onLayerNear.near(pieces.get(onLayer.get(first)).shape().box(), widest)) {
                    if (second <= first) {
                        continue;
                    }
                    int oneIndex = onLayer.get(first);
                    int otherIndex = onLayer.get(second);
                    CopperPieces.Piece one = pieces.get(oneIndex);
                    CopperPieces.Piece other = pieces.get(otherIndex);
                    if (one.group() == other.group() || (one.net().isPresent() && one.net().equals(other.net()))
                        || (one.part().isPresent() && one.part().equals(other.part())) || !one.shape().within(other.shape(), widest)
                        || !compared.add((long) oneIndex * pieces.size() + otherIndex)) {
                        continue;
                    }
                    double needed = Math.max(rules.clearanceOf(one.net()), rules.clearanceOf(other.net()));
                    CopperShape.Gap gap = one.shape().gapTo(other.shape());
                    if (gap.gap() >= needed - settings.tolerance()) {
                        continue;
                    }
                    if (findings.size() >= settings.namedGaps()) {
                        unnamed++;
                        continue;
                    }
                    String where = layerTitle(layer).toLowerCase(Locale.ROOT);
                    findings.add(new PcbFinding(ERROR, gap.gap() <= 0
                            ? named(one) + " touches " + named(other) + " on " + where
                            : named(one) + " is " + said(gap.gap()) + " from " + named(other) + " on " + where + ": " + said(needed) + " is needed",
                            new ArrayList<>(new LinkedHashSet<>(List.of(one.key(), other.key()))), gap.at()));
                }
            }
        }
        if (unnamed > 0) {
            findings.add(new PcbFinding(ERROR, unnamed + " more gaps are too narrow", List.of()));
        }
        return findings;
    }

    /** Tracks thinner, vias smaller and rings thinner than the pcb is made with; holes for leads smaller than it drills. */
    private List<PcbFinding> sizeFindings(Map<String, Double> values) {
        List<PcbFinding> findings = new ArrayList<>();
        double leastTrack = rule(values, PcbVocabulary.RULE_LEAST_TRACK_WIDTH);
        List<Integer> thin = indexesWhere(pcb.tracks(), track -> track.width() < leastTrack - settings.tolerance());
        if (!thin.isEmpty()) {
            findings.add(new PcbFinding(ERROR, (thin.size() == 1 ? "Track " + (thin.getFirst() + 1) + " is" : thin.size() + " tracks are")
                    + " thinner than the " + said(leastTrack) + " the PCB is made with", keys("track", thin)));
        }
        double leastDrill = rule(values, PcbVocabulary.RULE_LEAST_VIA_DRILL);
        List<Integer> smallDrill = indexesWhere(pcb.vias(), via -> via.drill() < leastDrill - settings.tolerance());
        if (!smallDrill.isEmpty()) {
            findings.add(new PcbFinding(ERROR, (smallDrill.size() == 1 ? "Via " + (smallDrill.getFirst() + 1) + " is" : smallDrill.size() + " vias are")
                    + " drilled smaller than " + said(leastDrill), keys("via", smallDrill)));
        }
        double leastRing = rule(values, PcbVocabulary.RULE_LEAST_VIA_RING);
        List<Integer> thinRing = indexesWhere(pcb.vias(), via -> (via.diameter() - via.drill()) / 2 < leastRing - settings.tolerance());
        if (!thinRing.isEmpty()) {
            findings.add(new PcbFinding(ERROR, (thinRing.size() == 1 ? "Via " + (thinRing.getFirst() + 1) + " has" : thinRing.size() + " vias have")
                    + " a ring thinner than " + said(leastRing), keys("via", thinRing)));
        }
        double leastHole = rule(values, PcbVocabulary.RULE_LEAST_DRILL);
        List<Integer> smallHoles = indexesWhere(pcb.placements(), placement -> footprintOf(placement)
                .map(footprint -> footprint.pads().stream().anyMatch(pad -> pad.drillWidth() > 0 && pad.drillWidth() < leastHole - settings.tolerance()))
                .orElse(false));
        if (!smallHoles.isEmpty()) {
            findings.add(new PcbFinding(WARNING, smallHoles.stream().map(this::printed).collect(Collectors.joining(", "))
                    + (smallHoles.size() == 1 ? " has" : " have") + " holes smaller than the " + said(leastHole) + " the PCB is drilled with",
                    keys("placement", smallHoles)));
        }
        return findings;
    }

    /** A hole to measure: what it is named by, whose part it is, and its shape. */
    private record Hole(String key, String label, Optional<String> part, CopperShape shape) {
    }

    /** Holes nearer each other than the pcb's drill keeps between them — a part's own holes are its footprint's to keep. */
    private List<PcbFinding> holeFindings(Map<String, Double> values) {
        List<Hole> holes = new ArrayList<>();
        for (int index = 0; index < pcb.placements().size(); index++) {
            Placement placement = pcb.placements().get(index);
            int at = index;
            footprintOf(placement).ifPresent(footprint -> footprint.pads().stream().filter(pad -> pad.drillWidth() > 0)
                    .forEach(pad -> holes.add(new Hole(key("placement", at), "the hole of " + placement.printed() + " pad "
                            + (pad.number().isEmpty() ? "—" : pad.number()), Optional.of(placement.designator()), CopperShapes.hole(placement, pad)))));
        }
        for (int index = 0; index < pcb.vias().size(); index++) {
            Via via = pcb.vias().get(index);
            holes.add(new Hole(key("via", index), "via " + (index + 1), Optional.empty(), CopperShape.circle(via.x(), via.y(), via.drill() / 2)));
        }
        for (int index = 0; index < pcb.holes().size(); index++) {
            org.jmouse.cad.pcb.drawing.Hole hole = pcb.holes().get(index);
            holes.add(new Hole(key("hole", index), "hole " + (index + 1), Optional.empty(), CopperShape.circle(hole.x(), hole.y(), hole.drill() / 2)));
        }
        double keep = rule(values, PcbVocabulary.RULE_HOLE_CLEARANCE);
        List<PcbFinding> findings = new ArrayList<>();
        BoxIndex holesNear = BoxTree.of(holes.stream().map(hole -> hole.shape().box()).toList());
        for (int first = 0; first < holes.size(); first++) {
            for (int second : holesNear.near(holes.get(first).shape().box(), keep)) {
                if (second <= first) {
                    continue;
                }
                Hole one = holes.get(first);
                Hole other = holes.get(second);
                if ((one.part().isPresent() && one.part().equals(other.part())) || !one.shape().within(other.shape(), keep)) {
                    continue;
                }
                CopperShape.Gap gap = one.shape().gapTo(other.shape());
                if (gap.gap() < keep - settings.tolerance()) {
                    String label = Character.toUpperCase(one.label().charAt(0)) + one.label().substring(1);
                    findings.add(new PcbFinding(ERROR, label + " is " + said(gap.gap()) + " from " + other.label() + ": holes keep "
                            + said(keep) + " apart", new ArrayList<>(new LinkedHashSet<>(List.of(one.key(), other.key()))), gap.at()));
                }
            }
        }
        return findings;
    }

    // ─── the edge ──────────────────────────────────────────────────────────────────────────────────────

    /** An edge that does not close: there is no telling what is inside it, and no factory can cut it. */
    private List<PcbFinding> openEdgeFindings() {
        List<PcbFinding> findings = new ArrayList<>();
        List<String> outlines = PathOutlines.subpaths(pcb.outline());
        for (int index = 0; index < outlines.size(); index++) {
            String outline = outlines.get(index);
            if (PathOutlines.isClosed(outline)) {
                continue;
            }
            List<Point> ends = PathOutlines.openEnds(outline, settings.curveSteps());
            if (ends.isEmpty()) {
                continue;
            }
            findings.add(new PcbFinding(ERROR, "The PCB's edge is open between " + ends.stream().map(point -> written(rounded(point.x(), 3)) + ", " + written(rounded(point.y(), 3)))
                    .collect(Collectors.joining(" and ")) + " mm: it cannot be cut", List.of(key("outline", index)), ends.getFirst()));
        }
        return findings;
    }

    /** Copper nearer the pcb's edge than it may come, or beyond the edge altogether. */
    private List<PcbFinding> edgeFindings(Map<String, Double> values) {
        Optional<CopperShape> edge = CopperShape.ofPath(pcb.outline(), true, 0, settings.curveSteps());
        if (edge.isEmpty()) {
            return pcb.placements().isEmpty() ? List.of() : List.of(new PcbFinding(WARNING, "The PCB has no edge", List.of()));
        }
        CopperShape line = edge.get().lineOnly();
        double keep = rule(values, PcbVocabulary.RULE_EDGE_CLEARANCE);
        Set<String> outside = new LinkedHashSet<>();
        Set<String> near = new LinkedHashSet<>();
        Point nearAt = null;
        for (CopperPieces.Piece piece : pieces) {
            double[] first = piece.shape().pieces().getFirst();
            if (!edge.get().encloses(first[0], first[1])) {
                outside.add(piece.key());
                continue;
            }
            if (!piece.shape().within(line, keep)) {
                continue;
            }
            CopperShape.Gap gap = piece.shape().gapTo(line);
            if (gap.gap() < keep - settings.tolerance()) {
                near.add(piece.key());
                nearAt = nearAt == null ? gap.at() : nearAt;
            }
        }
        List<PcbFinding> findings = new ArrayList<>();
        if (!outside.isEmpty()) {
            findings.add(new PcbFinding(ERROR, (outside.size() == 1 ? "A piece of copper stands" : outside.size() + " pieces of copper stand")
                    + " beyond the PCB's edge", List.copyOf(outside)));
        }
        if (!near.isEmpty()) {
            findings.add(new PcbFinding(ERROR, (near.size() == 1 ? "A piece of copper comes" : near.size() + " pieces of copper come")
                    + " nearer the edge than " + said(keep), List.copyOf(near), nearAt));
        }
        return findings;
    }

    // ─── areas ─────────────────────────────────────────────────────────────────────────────────────────

    /** Copper standing in a keepout, on a layer the keepout keeps clear. */
    private List<PcbFinding> keepoutFindings() {
        List<PcbFinding> findings = new ArrayList<>();
        for (int index = 0; index < pcb.keepouts().size(); index++) {
            Keepout keepout = pcb.keepouts().get(index);
            Optional<CopperShape> area = CopperShape.ofPath(keepout.path(), true, 0, settings.curveSteps());
            if (area.isEmpty()) {
                continue;
            }
            List<CopperPieces.Piece> inside = Arrays.stream(piecesNear.near(area.get().box(), 0)).mapToObj(pieces::get)
                    .filter(piece -> keepout.layers().stream().anyMatch(piece.onLayer())
                    && piece.shape().within(area.get(), 0) && piece.shape().comesWithin(area.get(), 0)).toList();
            if (!inside.isEmpty()) {
                List<String> keys = new ArrayList<>(List.of(key("keepout", index)));
                inside.forEach(piece -> keys.add(piece.key()));
                findings.add(new PcbFinding(ERROR, (inside.size() == 1 ? inside.getFirst().label() + " stands" : inside.size() + " pieces of copper stand")
                        + " in keepout " + (index + 1), keys));
            }
        }
        return findings;
    }

    /** Zones poured for a net the circuit does not have: copper that joins nothing it was meant to. */
    private List<PcbFinding> zoneFindings() {
        Set<String> circuitNets = subject.partNets().values().stream().flatMap(nets -> nets.values().stream()).collect(Collectors.toSet());
        List<PcbFinding> findings = new ArrayList<>();
        for (int index = 0; index < pcb.zones().size(); index++) {
            Zone zone = pcb.zones().get(index);
            if (!circuitNets.contains(zone.net())) {
                findings.add(new PcbFinding(WARNING, "Zone " + (index + 1) + " is poured for " + zone.net() + ", a net the circuit does not have",
                        List.of(key("zone", index))));
            }
        }
        return findings;
    }

    // ─── parts and markings ────────────────────────────────────────────────────────────────────────────

    /** The box of a part with no courtyard: far from anything, so no courtyard is ever found near it. */
    private static final Box NO_COURTYARD = new Box(Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE);

    /** Parts on one side whose courtyards overlap: the room a part needs to be placed and soldered is taken. */
    private List<PcbFinding> courtyardFindings() {
        List<Optional<Box>> boxes = pcb.placements().stream().map(this::courtyardBox).toList();
        List<PcbFinding> findings = new ArrayList<>();
        // Parts with no courtyard stand nowhere: an empty box, which no other box comes near.
        BoxIndex courtyardsNear = BoxTree.of(boxes.stream().map(box -> box.orElse(NO_COURTYARD)).toList());
        for (int first = 0; first < boxes.size(); first++) {
            if (boxes.get(first).isEmpty()) {
                continue;
            }
            for (int second : courtyardsNear.near(boxes.get(first).get(), 0)) {
                if (second <= first) {
                    continue;
                }
                Placement placement = pcb.placements().get(first);
                Placement other = pcb.placements().get(second);
                if (boxes.get(first).isEmpty() || boxes.get(second).isEmpty() || !placement.side().equals(other.side())) {
                    continue;
                }
                Box one = boxes.get(first).get();
                Box two = boxes.get(second).get();
                if (one.overlaps(two)) {
                    Point at = new Point((Math.max(one.left(), two.left()) + Math.min(one.right(), two.right())) / 2,
                            (Math.max(one.top(), two.top()) + Math.min(one.bottom(), two.bottom())) / 2);
                    findings.add(new PcbFinding(WARNING, placement.printed() + " and " + other.printed() + " crowd each other: their courtyards overlap",
                            List.of(key("placement", first), key("placement", second)), at));
                }
            }
        }
        return findings;
    }

    /** The box a placed part's courtyard covers; empty for a footprint that draws none. */
    private Optional<Box> courtyardBox(Placement placement) {
        List<FootprintDrawing.Outline> outlines = footprintOf(placement).map(footprint -> footprint.outlines().stream()
                .filter(outline -> PcbVocabulary.COURTYARD.equals(outline.layer())).toList()).orElse(List.of());
        double left = Double.POSITIVE_INFINITY;
        double top = Double.POSITIVE_INFINITY;
        double right = Double.NEGATIVE_INFINITY;
        double bottom = Double.NEGATIVE_INFINITY;
        for (FootprintDrawing.Outline outline : outlines) {
            Optional<Box> drawn = pathBox(outline.path());
            if (drawn.isEmpty()) {
                continue;
            }
            Box box = drawn.get();
            for (double[] corner : new double[][] {{box.left(), box.top()}, {box.right(), box.top()}, {box.left(), box.bottom()}, {box.right(), box.bottom()}}) {
                Point placed = placement.placed(corner[0], corner[1]);
                left = Math.min(left, placed.x());
                right = Math.max(right, placed.x());
                top = Math.min(top, placed.y());
                bottom = Math.max(bottom, placed.y());
            }
        }
        return left > right ? Optional.empty() : Optional.of(new Box(left, top, right, bottom));
    }

    /** A marking that may be printed cut: what it is named by and as, its side, the box it covers and its corners. */
    private record Marking(String key, String label, String side, Box box, List<Point> corners) {
    }

    /**
     * Silkscreen a pcb house would print cut: a designator or a text lying over a pad on its own side or over a mounting
     * hole — no ink is printed on bare copper or into a hole —, or standing beyond the pcb's edge, where it is milled away.
     * One finding a marking, naming the first thing it covers.
     */
    private List<PcbFinding> silkscreenFindings() {
        List<Marking> markings = new ArrayList<>();
        for (int index = 0; index < pcb.placements().size(); index++) {
            Placement placement = pcb.placements().get(index);
            PlacedDesignator standing = PlacedDesignator.of(placement, footprintOf(placement), settings.designator());
            if (!standing.printed()) {
                continue;
            }
            Box box = standing.bounds(placement.designator(), settings.designator());
            markings.add(new Marking(key("designator", index), placement.printed() + "'s designator", placement.side(), box, cornersOf(box)));
        }
        int textIndex = 0;
        for (Lettering text : pcb.texts()) {
            if (text.layer().endsWith(SILKSCREEN_SUFFIX)) {
                List<Point> corners = textCorners(text);
                markings.add(new Marking(key("text", textIndex), "Text \"" + text.text() + "\"",
                        text.layer().substring(0, text.layer().length() - SILKSCREEN_SUFFIX.length()), boxOf(corners), corners));
            }
            textIndex++;
        }
        Optional<CopperShape> edge = CopperShape.ofPath(pcb.outline(), true, 0, settings.curveSteps());
        List<PcbFinding> findings = new ArrayList<>();
        for (Marking marking : markings) {
            Point at = new Point((marking.box().left() + marking.box().right()) / 2, (marking.box().top() + marking.box().bottom()) / 2);
            Optional<CopperNets.PlacedPad> pad = nets.pads().stream()
                    .filter(candidate -> (candidate.pad().isPlatedThrough() || candidate.pad().hasCopperOn(marking.side(), candidate.placement()))
                                         && marking.box().overlaps(padBox(candidate)))
                    .findFirst();
            if (pad.isPresent()) {
                findings.add(new PcbFinding(WARNING, marking.label() + " lies over " + pad.get().placement().printed() + " pad "
                        + pad.get().pad().number() + ": it would be printed cut", List.of(marking.key()), at));
                continue;
            }
            Optional<Integer> hole = firstIndexWhere(pcb.holes(), candidate -> marking.box().overlaps(holeBox(candidate)));
            if (hole.isPresent()) {
                findings.add(new PcbFinding(WARNING, marking.label() + " lies over hole " + (hole.get() + 1) + ": it would be printed cut",
                        List.of(marking.key(), key("hole", hole.get())), at));
                continue;
            }
            if (edge.isPresent() && marking.corners().stream().anyMatch(corner -> !edge.get().encloses(corner.x(), corner.y()))) {
                findings.add(new PcbFinding(WARNING, marking.label() + " runs beyond the PCB's edge: it would be milled away", List.of(marking.key()), at));
            }
        }
        return findings;
    }

    /** Track ends that reach no pad, via or other track on their layer: copper left hanging. */
    private List<PcbFinding> looseEndFindings() {
        List<PcbFinding> findings = new ArrayList<>();
        for (int index = 0; index < pcb.tracks().size(); index++) {
            Track track = pcb.tracks().get(index);
            String own = key("track", index);
            for (Point end : List.of(track.points().getFirst(), track.points().getLast())) {
                CopperShape point = CopperShape.circle(end.x(), end.y(), 0);
                boolean reached = Arrays.stream(piecesNear.near(point.box(), 0)).mapToObj(pieces::get)
                        .anyMatch(piece -> !piece.key().equals(own) && piece.onLayer().test(track.layer())
                        && piece.shape().within(point, 0) && piece.shape().comesWithin(point, settings.tolerance()));
                if (!reached) {
                    findings.add(new PcbFinding(WARNING, "Track " + (index + 1) + " ends at " + written(rounded(end.x(), 2)) + ", " + written(rounded(end.y(), 2))
                            + " joined to nothing", List.of(own), end));
                }
            }
        }
        return findings;
    }

    // ─── pairs ─────────────────────────────────────────────────────────────────────────────────────────

    /** A pair's tracks of another width, its two nets further apart in length than it allows, its gap nearer than the clearance. */
    private List<PcbFinding> pairFindings() {
        if (pcb.pairs().isEmpty()) {
            return List.of();
        }
        double thickness = subject.values().map(values -> values.getOrDefault(PcbVocabulary.RULE_PCB_THICKNESS, 0.0)).orElse(0.0);
        Map<String, NetLengths.Length> lengths = NetLengths.of(pcb, nets, thickness);
        List<PcbFinding> findings = new ArrayList<>();
        for (Pair pair : pcb.pairs()) {
            List<String> otherWidth = new ArrayList<>();
            for (int index = 0; index < pcb.tracks().size(); index++) {
                Optional<String> net = nets.trackNets().get(index);
                if (net.filter(found -> found.equals(pair.positive()) || found.equals(pair.negative())).isPresent()
                    && Math.abs(pcb.tracks().get(index).width() - pair.width()) > 1e-4) {
                    otherWidth.add(key("track", index));
                }
            }
            if (!otherWidth.isEmpty()) {
                findings.add(new PcbFinding(WARNING, otherWidth.size() + " track" + (otherWidth.size() == 1 ? "" : "s") + " of the pair " + pair.name()
                        + " not " + written(pair.width()) + " mm wide", otherWidth));
            }
            double positive = Optional.ofNullable(lengths.get(pair.positive())).map(NetLengths.Length::total).orElse(0.0);
            double negative = Optional.ofNullable(lengths.get(pair.negative())).map(NetLengths.Length::total).orElse(0.0);
            if (pair.skew() > 0 && positive > 0 && negative > 0 && Math.abs(positive - negative) > pair.skew()) {
                List<String> keys = new ArrayList<>(copperOf(pair.positive()));
                keys.addAll(copperOf(pair.negative()));
                findings.add(new PcbFinding(WARNING, "The pair " + pair.name() + " is " + String.format(Locale.ROOT, "%.3f", Math.abs(positive - negative))
                        + " mm apart in length; it allows " + written(pair.skew()) + " mm", keys));
            }
            subject.values().map(values -> rule(values, PcbVocabulary.RULE_CLEARANCE)).filter(clearance -> pair.gap() < clearance)
                    .ifPresent(clearance -> findings.add(new PcbFinding(WARNING, "The pair " + pair.name() + " is laid " + written(pair.gap())
                            + " mm apart, nearer than the PCB's clearance of " + written(clearance) + " mm", List.of())));
        }
        return findings;
    }

    /** The tracks, arcs and vias on a net. */
    private List<String> copperOf(String net) {
        List<String> keys = new ArrayList<>();
        keys.addAll(keysOnNet("track", nets.trackNets(), net));
        keys.addAll(keysOnNet("arc", nets.arcNets(), net));
        keys.addAll(keysOnNet("via", nets.viaNets(), net));
        return keys;
    }

    // ─── helpers ───────────────────────────────────────────────────────────────────────────────────────

    private static final String SILKSCREEN_SUFFIX = "-" + PcbVocabulary.SILKSCREEN;

    private Optional<FootprintDrawing> footprintOf(Placement placement) {
        return Optional.ofNullable(subject.footprints().get(placement.designator()));
    }

    private String printed(int placement) {
        return pcb.placements().get(placement).printed();
    }

    private static double rule(Map<String, Double> values, String word) {
        return values.getOrDefault(word, 0.0);
    }

    private static String named(CopperPieces.Piece piece) {
        return piece.net().map(net -> piece.label() + " (" + net + ")").orElse(piece.label());
    }

    /** What a copper layer is called in a finding: {@code Top copper}, {@code Inner copper 1}, {@code Bottom copper}. */
    private static String layerTitle(String layer) {
        if (layer.startsWith(INNER_PREFIX)) {
            return "Inner copper " + layer.substring(INNER_PREFIX.length());
        }
        return Character.toUpperCase(layer.charAt(0)) + layer.substring(1) + " copper";
    }

    /** What an inner copper layer's id opens with, before its number: {@code inner-1}. */
    private static final String INNER_PREFIX = "inner-";

    /** A length as a person reads it in a finding: to a thousandth, trailing zeros dropped, never below 0. */
    private static String said(double millimetres) {
        return written(Math.max(0, rounded(millimetres, 3))) + " mm";
    }

    private static double rounded(double value, int places) {
        return BigDecimal.valueOf(value).setScale(places, RoundingMode.HALF_UP).doubleValue();
    }

    /** A number written as the interface writes one: no trailing zeros, no exponent. */
    private static String written(double value) {
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }

    private static <T> List<Integer> indexesWhere(List<T> items, java.util.function.Predicate<T> wanted) {
        List<Integer> indexes = new ArrayList<>();
        for (int index = 0; index < items.size(); index++) {
            if (wanted.test(items.get(index))) {
                indexes.add(index);
            }
        }
        return indexes;
    }

    private static <T> Optional<Integer> firstIndexWhere(List<T> items, java.util.function.Predicate<T> wanted) {
        List<Integer> found = indexesWhere(items, wanted);
        return found.isEmpty() ? Optional.empty() : Optional.of(found.getFirst());
    }

    private static List<String> keys(String kind, List<Integer> indexes) {
        return indexes.stream().map(index -> key(kind, index)).toList();
    }

    private static List<String> keysOnNet(String kind, List<Optional<String>> nets, String net) {
        return indexesWhere(nets, found -> found.filter(net::equals).isPresent()).stream().map(index -> key(kind, index)).toList();
    }

    /** The box a placed pad covers: its width and height, turned a quarter where its turn and its part's make one. */
    private static Box padBox(CopperNets.PlacedPad pad) {
        double turned = (((pad.pad().rotation() + pad.placement().rotation()) % 180) + 180) % 180;
        double across = turned == 90 ? pad.pad().height() : pad.pad().width();
        double down = turned == 90 ? pad.pad().width() : pad.pad().height();
        return new Box(pad.centre().x() - across / 2, pad.centre().y() - down / 2, pad.centre().x() + across / 2, pad.centre().y() + down / 2);
    }

    private static Box holeBox(org.jmouse.cad.pcb.drawing.Hole hole) {
        double reach = CopperShapes.reach(hole);
        return new Box(hole.x() - reach, hole.y() - reach, hole.x() + reach, hole.y() + reach);
    }

    /** A text's four corners, turned as it stands: as wide as its letters advance, as tall as its size, on its point by its anchor. */
    private List<Point> textCorners(Lettering text) {
        double width = text.text().length() * text.size() * settings.designator().advance();
        double left = PcbVocabulary.TEXT_START.equals(text.anchor()) ? 0 : PcbVocabulary.TEXT_END.equals(text.anchor()) ? -width : -width / 2;
        double half = text.size() / 2;
        double angle = Math.toRadians(text.rotation());
        double cosine = Math.cos(angle);
        double sine = Math.sin(angle);
        Function<double[], Point> turned = corner -> new Point(text.x() + corner[0] * cosine + corner[1] * sine,
                text.y() - corner[0] * sine + corner[1] * cosine);
        return Stream.of(new double[] {left, -half}, new double[] {left + width, -half}, new double[] {left + width, half}, new double[] {left, half})
                .map(turned).toList();
    }

    private static Box boxOf(List<Point> corners) {
        return new Box(corners.stream().mapToDouble(Point::x).min().orElse(0),
                corners.stream().mapToDouble(Point::y).min().orElse(0),
                corners.stream().mapToDouble(Point::x).max().orElse(0),
                corners.stream().mapToDouble(Point::y).max().orElse(0));
    }

    private static List<Point> cornersOf(Box box) {
        return List.of(new Point(box.left(), box.top()), new Point(box.right(), box.top()),
                new Point(box.right(), box.bottom()), new Point(box.left(), box.bottom()));
    }

    /** The box a path covers. */
    private Optional<Box> pathBox(String path) {
        if (path == null || path.isBlank()) {
            return Optional.empty();
        }
        List<double[]> points = PathPolylines.of(PATHS.parse(path), settings.curveSteps()).stream().flatMap(List::stream).toList();
        if (points.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new Box(points.stream().mapToDouble(point -> point[0]).min().orElseThrow(),
                points.stream().mapToDouble(point -> point[1]).min().orElseThrow(),
                points.stream().mapToDouble(point -> point[0]).max().orElseThrow(),
                points.stream().mapToDouble(point -> point[1]).max().orElseThrow()));
    }
}
