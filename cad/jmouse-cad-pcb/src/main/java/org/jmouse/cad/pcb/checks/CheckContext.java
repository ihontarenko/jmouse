package org.jmouse.cad.pcb.checks;

import org.jmouse.cad.Point;
import org.jmouse.cad.pcb.PcbVocabulary;
import org.jmouse.cad.pcb.drawing.FootprintDrawing;
import org.jmouse.cad.pcb.drawing.Lettering;
import org.jmouse.cad.pcb.drawing.PcbDrawing;
import org.jmouse.cad.pcb.drawing.Placement;
import org.jmouse.cad.pcb.geometry.CopperNets;
import org.jmouse.cad.pcb.geometry.CopperPieces;
import org.jmouse.geometry.index.Box;
import org.jmouse.geometry.index.BoxIndex;
import org.jmouse.geometry.index.BoxTree;
import org.jmouse.svg.path.node.PathPolylines;
import org.jmouse.svg.path.parser.PathParser;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.jmouse.cad.pcb.geometry.CopperPieces.key;

/**
 * What every check reads of one pcb, worked out once for them all: the pcb and what it is checked against, its copper's nets and
 * pieces, the pieces by their boxes and by their kinds — and what a check asks of them.
 */
public final class CheckContext {

    private static final PathParser PATHS = new PathParser();

    private final PcbChecks.Subject                                     subject;
    private final PcbChecks.Settings                                    settings;
    private final CopperNets                                            nets;
    private final List<CopperPieces.Piece>                              pieces;
    /** The pieces by their boxes: what stands near a piece found among its neighbours, never every piece of the pcb. */
    private final BoxIndex                                              piecesNear;
    /** Each piece by its kind and its place among its kind's: a member of the joining found at once. */
    private final Map<CopperPieces.Kind, Map<Integer, CopperPieces.Piece>> piecesByKind = new EnumMap<>(CopperPieces.Kind.class);

    CheckContext(PcbChecks.Subject subject, PcbChecks.Settings settings, CopperNets nets, List<CopperPieces.Piece> pieces) {
        this.subject = subject;
        this.settings = settings;
        this.nets = nets;
        this.pieces = pieces;
        this.piecesNear = BoxTree.of(pieces.stream().map(piece -> piece.shape().box()).toList());
        pieces.forEach(piece -> piecesByKind.computeIfAbsent(piece.kind(), kind -> new HashMap<>()).putIfAbsent(piece.index(), piece));
    }

    public PcbChecks.Subject subject() {
        return subject;
    }

    public PcbChecks.Settings settings() {
        return settings;
    }

    public PcbDrawing pcb() {
        return subject.pcb();
    }

    public CopperNets nets() {
        return nets;
    }

    public List<CopperPieces.Piece> pieces() {
        return pieces;
    }

    public BoxIndex piecesNear() {
        return piecesNear;
    }

    /** The values the pcb is held to, by rule word; empty while they are not known. */
    public Optional<Map<String, Double>> values() {
        return subject.values();
    }

    /** The piece of copper a member of the joining is, where the checks measure one — a zone's pour is not among them. */
    public Optional<CopperPieces.Piece> pieceOf(CopperNets.Member member) {
        if (member.kind() == CopperNets.MemberKind.ZONE) {
            return Optional.empty();
        }
        CopperPieces.Kind kind = CopperPieces.Kind.valueOf(member.kind().name());
        return Optional.ofNullable(piecesByKind.getOrDefault(kind, Map.of()).get(member.index()));
    }

    /** What a member of the joining is named by in a finding. */
    public String keyOf(CopperNets.Member member) {
        return member.kind() == CopperNets.MemberKind.ZONE ? key("zone", member.index())
                : pieceOf(member).map(CopperPieces.Piece::key).orElse(key(member.kind().name().toLowerCase(Locale.ROOT), member.index()));
    }

    /** A placed part's footprint; empty where the library does not have it. */
    public Optional<FootprintDrawing> footprintOf(Placement placement) {
        return Optional.ofNullable(subject.footprints().get(placement.designator()));
    }

    /** A placement as a finding prints it, by its place. */
    public String printed(int placement) {
        return pcb().placements().get(placement).printed();
    }

    /** The tracks, arcs and vias on a net. */
    public List<String> copperOf(String net) {
        List<String> keys = new ArrayList<>();
        keys.addAll(FindingWords.keysOnNet("track", nets.trackNets(), net));
        keys.addAll(FindingWords.keysOnNet("arc", nets.arcNets(), net));
        keys.addAll(FindingWords.keysOnNet("via", nets.viaNets(), net));
        return keys;
    }

    /** The box a path covers. */
    public Optional<Box> pathBox(String path) {
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

    /** A text's four corners, turned as it stands: as wide as its letters advance, as tall as its size, on its point by its anchor. */
    public List<Point> textCorners(Lettering text) {
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
}
