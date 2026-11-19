package org.jmouse.cad.pcb.drawing;

import org.jmouse.cad.pcb.PcbWords;
import org.jmouse.cad.pcb.PcbLinesParser;
import org.jmouse.cad.pcb.PcbVocabulary;
import org.jmouse.cad.document.ArrangementLines;
import org.jmouse.cad.document.PathTextNode;
import org.jmouse.el.grammar.ClauseLineNode;
import org.jmouse.el.grammar.LineBuilder;
import org.jmouse.el.grammar.Nodes;
import org.jmouse.el.grammar.PointNode;
import org.jmouse.svg.path.parser.PathParseException;
import org.jmouse.svg.path.parser.PathParser;
import org.jmouse.el.grammar.ConnectionsNode;
import java.util.ArrayList;
import java.util.List;
import org.jmouse.cad.Point;

/**
 * A pcb written as its document's lines, built on the grammars that read them, in the order a pcb is written: its circuit, how it is
 * made, its classes, pairs and teardrops, its layers and edge, its parts, then its copper, keepouts, dimensions, texts and holes. A
 * value that is its line's default — no rotation, the top side — is not written. {@link PcbDocumentReading} reads them back.
 */
public final class PcbDocumentWriting {

    private PcbDocumentWriting() {
    }

    private static final PathParser PATHS = new PathParser();

    /**
     * The pcb as its document's lines, built on the grammars that read them, in the order a pcb is written: its
     * circuit, how it is made, its classes, pairs and teardrops, its layers and edge, its parts, then its copper, keepouts,
     * dimensions, texts and holes. A value that is its line's default — no rotation, the top side — is not written.
     *
     * @throws IllegalArgumentException when a path does not read as path data
     */
    static List<ClauseLineNode> written(PcbDrawing pcb) {
        List<ClauseLineNode> lines = new ArrayList<>();
        if (pcb.circuit() != null && !pcb.circuit().isEmpty()) {
            lines.add(line(PcbVocabulary.CIRCUIT).slot(PcbWords.SLUG, Nodes.word(pcb.circuit())).build());
        }
        pcb.rules().ifPresent(made -> {
            LineBuilder line = line("rules").slot(PcbWords.PROFILE, Nodes.word(made.profile()));
            made.changes().forEach((rule, value) -> line.clause(rule, Nodes.number(value)));
            lines.add(line.build());
        });
        for (NetClass netClass : pcb.netClasses()) {
            LineBuilder line = line("class").slot(PcbWords.NAME, Nodes.word(netClass.name()))
                    .clause(PcbWords.WIDTH, Nodes.number(netClass.width())).clause(PcbWords.CLEARANCE, Nodes.number(netClass.clearance()));
            if (netClass.drill() > 0) {
                line.clause(PcbWords.DRILL, Nodes.number(netClass.drill()));
            }
            if (netClass.diameter() > 0) {
                line.clause(PcbWords.DIAMETER, Nodes.number(netClass.diameter()));
            }
            lines.add(line.clause(PcbWords.NETS, Nodes.words(netClass.nets())).build());
        }
        for (Pair pair : pcb.pairs()) {
            LineBuilder line = line("pair").slot(PcbWords.NAME, Nodes.word(pair.name())).clause(PcbWords.WIDTH, Nodes.number(pair.width()))
                    .clause(PcbWords.GAP, Nodes.number(pair.gap()));
            if (pair.skew() > 0) {
                line.clause(PcbWords.SKEW, Nodes.number(pair.skew()));
            }
            lines.add(line.clause(PcbWords.NETS, Nodes.words(List.of(pair.positive(), pair.negative()))).build());
        }
        if (!pcb.sizes().tracks().isEmpty()) {
            lines.add(line("sizes").slot(PcbWords.SIZED, Nodes.word(PcbVocabulary.TRACKS)).slot(PcbWords.WIDTHS, Nodes.numbers(pcb.sizes().tracks())).build());
        }
        if (!pcb.sizes().vias().isEmpty()) {
            lines.add(line("sizes").slot(PcbWords.SIZED, Nodes.word(PcbVocabulary.VIAS))
                              .clause(PcbWords.DRILL, Nodes.numbers(pcb.sizes().vias().stream().map(ViaSize::drill).toList()))
                              .clause(PcbWords.DIAMETER, Nodes.numbers(pcb.sizes().vias().stream().map(ViaSize::diameter).toList())).build());
        }
        for (Teardrop teardrop : pcb.teardrops()) {
            LineBuilder line = line("teardrops").slot(PcbWords.TARGET, Nodes.word(teardrop.target()))
                    .clause(PcbWords.LENGTH, Nodes.number(teardrop.length())).clause(PcbWords.WIDTH, Nodes.number(teardrop.width()));
            if (teardrop.curved()) {
                line.flag(PcbWords.CURVED);
            }
            lines.add(line.build());
        }
        if (pcb.copperLayers() > 0) {
            lines.add(line("layers").slot(PcbWords.COUNT, Nodes.number(pcb.copperLayers())).build());
        }
        if (pcb.outline() != null && !pcb.outline().isEmpty()) {
            lines.add(line("edge").ending(pathText("the edge", pcb.outline())).build());
        }
        pcb.groups().forEach(group -> lines.add(ArrangementLines.groupLine(group)));
        pcb.placements().forEach(placement -> lines.add(placement(placement)));
        for (Track track : pcb.tracks()) {
            LineBuilder line = line("track").slot(PcbWords.LAYER, Nodes.word(track.layer())).clause(PcbWords.WIDTH, Nodes.number(track.width()));
            if (track.rounding() > 0) {
                line.clause(PcbWords.ROUNDING, Nodes.number(track.rounding()));
            }
            if (!track.corners().isEmpty()) {
                line.clause(PcbWords.CORNERS, Nodes.numbers(track.corners()));
            }
            line.clause(PcbWords.FROM, point(track.points().getFirst()));
            track.points().stream().skip(1).forEach(point -> line.clause(PcbWords.TO, point(point)));
            ArrangementLines.write(line, track.arrangement());
            finished(line, track.finish());
            lines.add(line.build());
        }
        for (Arc arc : pcb.arcs()) {
            LineBuilder line = line("arc").slot(PcbWords.LAYER, Nodes.word(arc.layer())).clause(PcbWords.WIDTH, Nodes.number(arc.width()))
                    .clause(PcbWords.FROM, point(arc.from())).clause(PcbWords.THROUGH, point(arc.through())).clause(PcbWords.TO, point(arc.to()));
            ArrangementLines.write(line, arc.arrangement());
            finished(line, arc.finish());
            lines.add(line.build());
        }
        for (Via via : pcb.vias()) {
            LineBuilder line = line("via").clause(PcbWords.AT, Nodes.point(via.x(), via.y())).clause(PcbWords.DRILL, Nodes.number(via.drill()))
                    .clause(PcbWords.DIAMETER, Nodes.number(via.diameter()));
            if (via.from() != null && !via.from().isEmpty()) {
                line.clause(PcbWords.FROM, Nodes.word(via.from()));
            }
            if (via.to() != null && !via.to().isEmpty()) {
                line.clause(PcbWords.TO, Nodes.word(via.to()));
            }
            if (via.open() != null && !via.open().isEmpty()) {
                line.clause(PcbWords.OPEN, Nodes.word(via.open()));
            }
            ArrangementLines.write(line, via.arrangement());
            lines.add(line.build());
        }
        for (Zone zone : pcb.zones()) {
            LineBuilder line = line("zone").slot(PcbWords.LAYER, Nodes.word(zone.layer())).clause(PcbWords.NET, Nodes.word(zone.net()));
            if (zone.clearance() > 0) {
                line.clause(PcbWords.CLEARANCE, Nodes.number(zone.clearance()));
            }
            if (zone.priority() > 0) {
                line.clause(PcbWords.PRIORITY, Nodes.number(zone.priority()));
            }
            finished(line, zone.finish());
            lines.add(line.ending(pathText("the zone on " + zone.layer(), zone.path())).build());
        }
        for (Keepout keepout : pcb.keepouts()) {
            lines.add(line("keepout").slot(PcbWords.LAYERS, Nodes.words(keepout.layers()))
                    .ending(pathText("the keepout", keepout.path())).build());
        }
        for (Dimension dimension : pcb.dimensions()) {
            LineBuilder line = line("dimension").clause(PcbWords.FROM, point(dimension.from())).clause(PcbWords.TO, point(dimension.to()));
            if (dimension.offset() != 0) {
                line.clause(PcbWords.OFFSET, Nodes.number(dimension.offset()));
            }
            lines.add(line.build());
        }
        for (Lettering text : pcb.texts()) {
            LineBuilder line = line("text").slot(PcbWords.LAYER, Nodes.word(text.layer())).clause(PcbWords.SIZE, Nodes.number(text.size()))
                    .clause(PcbWords.AT, Nodes.point(text.x(), text.y()));
            if (text.rotation() != 0) {
                line.clause(PcbWords.ROTATE, Nodes.number(text.rotation()));
            }
            if (!PcbVocabulary.TEXT_MIDDLE.equals(text.anchor())) {
                line.clause(PcbWords.ANCHOR, Nodes.word(text.anchor()));
            }
            lines.add(line.ending(Nodes.text(text.text())).build());
        }
        for (Shape shape : pcb.shapes()) {
            LineBuilder line = line("shape").slot(PcbWords.LAYER, Nodes.word(shape.layer()));
            if (shape.width() > 0) {
                line.clause(PcbWords.WIDTH, Nodes.number(shape.width()));
            }
            if (shape.filled()) {
                line.flag(PcbWords.FILLED);
            }
            if (!shape.net().isEmpty()) {
                line.clause(PcbWords.NET, Nodes.word(shape.net()));
            }
            lines.add(line.ending(pathText("the shape on " + shape.layer(), shape.path())).build());
        }
        for (Hole hole : pcb.holes()) {
            LineBuilder line = line("hole").clause(PcbWords.AT, Nodes.point(hole.x(), hole.y())).clause(PcbWords.DRILL, Nodes.number(hole.drill()));
            if (hole.ring() > 0) {
                line.clause(PcbWords.RING, Nodes.number(hole.ring()));
            }
            lines.add(line.build());
        }
        for (Output output : pcb.outputs()) {
            lines.add(line("output").slot(PcbWords.NAME, Nodes.word(output.name())).clause(PcbWords.FORMATS, Nodes.words(output.formats()))
                    .ending(Nodes.text(output.title())).build());
        }
        return lines;
    }

    /** What a line's copper is left as, said when it is not under the mask. */
    private static void finished(LineBuilder line, String finish) {
        if (!finish.isEmpty()) {
            line.clause(PcbWords.FINISH, Nodes.word(finish));
        }
    }

    /** {@code place R1 [shown R1] [footprint slug [nets (GND = 1, OUT = 2)]] at x, y [rotate r] [side bottom] [reference at x, y [rotate r]]}. */
    private static ClauseLineNode placement(Placement placement) {
        LineBuilder line = line("place").slot(PcbWords.REFERENCE, Nodes.word(placement.designator()));
        placement.shown().ifPresent(shown -> line.clause(PcbWords.SHOWN, Nodes.word(shown)));
        placement.footprint().ifPresent(footprint -> line.clause(PcbWords.FOOTPRINT, Nodes.word(footprint)));
        if (!placement.nets().isEmpty()) {
            List<ConnectionsNode.Connection> nets = new ArrayList<>();
            placement.nets().forEach((pad, net) -> nets.add(new ConnectionsNode.Connection(net, pad)));
            line.clause(PcbWords.NETS, new ConnectionsNode(nets));
        }
        line.clause(PcbWords.AT, Nodes.point(placement.x(), placement.y()));
        if (placement.rotation() != 0) {
            line.clause(PcbWords.ROTATE, Nodes.number(placement.rotation()));
        }
        if (placement.side() != null && !placement.side().equals(PcbVocabulary.TOP)) {
            line.clause(PcbWords.SIDE, Nodes.word(placement.side()));
        }
        ArrangementLines.write(line, placement.arrangement());
        if (!placement.role().isEmpty()) {
            line.clause(PcbWords.ROLE, Nodes.word(placement.role()));
        }
        placement.reference().ifPresent(reference -> {
            LineBuilder nested = LineBuilder.line(PcbLinesParser.grammar("place").clause(PcbWords.REFERENCE).nested())
                    .clause(PcbWords.AT, Nodes.point(reference.x(), reference.y()));
            if (reference.rotation() != 0) {
                nested.clause(PcbWords.ROTATE, Nodes.number(reference.rotation()));
            }
            if (reference.layer() != null && !reference.layer().isEmpty()) {
                nested.clause(PcbWords.LAYER, Nodes.word(reference.layer()));
            }
            line.clause(PcbWords.REFERENCE, nested.build().asHeader());
        });
        return line.build();
    }

    private static LineBuilder line(String word) {
        return LineBuilder.line(PcbLinesParser.grammar(word));
    }

    private static PointNode point(Point point) {
        return Nodes.point(point.x(), point.y());
    }

    private static PathTextNode pathText(String origin, String written) {
        try {
            return new PathTextNode(PATHS.parse(origin, written == null ? "" : written));
        } catch (PathParseException refused) {
            throw new IllegalArgumentException("the path data of " + origin + " does not read: " + refused.detail());
        }
    }
}
