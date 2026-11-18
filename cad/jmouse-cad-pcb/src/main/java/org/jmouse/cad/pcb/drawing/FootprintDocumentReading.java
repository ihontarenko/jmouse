package org.jmouse.cad.pcb.drawing;

import org.jmouse.cad.pcb.drawing.FootprintDrawing.Pad;
import org.jmouse.cad.pcb.drawing.FootprintDrawing.Outline;
import org.jmouse.cad.pcb.drawing.FootprintDrawing.Field;
import org.jmouse.cad.pcb.drawing.FootprintDrawing.Body;
import org.jmouse.cad.pcb.drawing.FootprintDrawing.Model;
import org.jmouse.cad.pcb.PcbWords;
import org.jmouse.cad.pcb.PcbVocabulary;
import org.jmouse.cad.pcb.FootprintLinesParser;
import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.document.PathTextNode;
import org.jmouse.el.grammar.ClauseLineNode;
import org.jmouse.el.grammar.ExtentNode;
import org.jmouse.el.grammar.LineGrammar;
import org.jmouse.el.grammar.PointNode;
import org.jmouse.el.grammar.WordNode;
import org.jmouse.el.node.Expression;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** A footprint document's lines read into a footprint, and a line's class as its editor draws it. {@link FootprintDocumentWriting} writes them. */
public final class FootprintDocumentReading {

    private FootprintDocumentReading() {
    }

    /** The lines a footprint editor draws: every line a footprint holds. */
    private static final Set<LineGrammar> DRAWN = Set.of(FootprintLinesParser.PAD, FootprintLinesParser.OUTLINE,
                                                         FootprintLinesParser.FIELD, FootprintLinesParser.MODEL);

    /** A line's class, as a footprint editor draws it — its word —; empty for a statement no footprint editor draws. */
    public static Optional<String> classOf(Expression statement) {
        return statement instanceof ClauseLineNode line && DRAWN.contains(line.grammar()) ? Optional.of(line.word())
                : Optional.empty();
    }

    /** The drawing a footprint document's lines make. */
    static FootprintDrawing read(DocumentNode footprint) {
        List<Pad> pads = new ArrayList<>();
        List<Outline> outlines = new ArrayList<>();
        List<Field> fields = new ArrayList<>();
        Body body = null;
        List<Model> models = new ArrayList<>();
        for (Expression statement : footprint.getExpressions()) {
            if (!(statement instanceof ClauseLineNode line)) {
                continue;
            }
            if (line.grammar() == FootprintLinesParser.PAD) {
                pads.add(pad(line));
            } else if (line.grammar() == FootprintLinesParser.OUTLINE) {
                outlines.add(new Outline(line.name(PcbWords.LAYER).orElseThrow(), line.quantity(PcbWords.WIDTH).orElse(0.0), line.has(PcbWords.FILLED),
                        path(line)));
            } else if (line.grammar() == FootprintLinesParser.FIELD) {
                PointNode at = line.point(PcbWords.AT).orElseThrow();
                fields.add(new Field(line.name(PcbWords.SUBJECT).orElseThrow(), at.x(), at.y(), line.quantity(PcbWords.SIZE).orElseThrow(),
                        line.quantity(PcbWords.ROTATE).orElse(0.0), line.word(PcbWords.LAYER).orElse(PcbVocabulary.SILKSCREEN),
                        line.word(PcbWords.ANCHOR).orElse(PcbVocabulary.TEXT_MIDDLE)));
            } else if (line.grammar() == FootprintLinesParser.BODY) {
                body = new Body(line.quantity(PcbWords.HEIGHT).orElseThrow());
            } else if (line.grammar() == FootprintLinesParser.MODEL) {
                models.add(Model.of(line));
            }
        }
        return new FootprintDrawing(pads, outlines, fields, body, models);
    }

    private static Pad pad(ClauseLineNode line) {
        PointNode at = line.point(PcbWords.AT).orElseThrow();
        ExtentNode size = (ExtentNode) line.value("size").orElseThrow();
        double drillWidth = 0;
        double drillHeight = 0;
        Expression drill = line.value("drill").orElse(null);
        if (drill instanceof ExtentNode oblong) {
            drillWidth = oblong.width();
            drillHeight = oblong.height();
        } else if (drill != null) {
            drillWidth = line.quantity(PcbWords.DRILL).orElseThrow();
            drillHeight = drillWidth;
        }
        String number = ((WordNode) line.slot(PcbWords.NUMBER).orElseThrow()).word();
        return new Pad(number, line.name(PcbWords.TYPE).orElseThrow(), line.name(PcbWords.SHAPE).orElseThrow(), at.x(), at.y(),
                line.quantity(PcbWords.ROTATE).orElse(0.0), size.width(), size.height(), drillWidth, drillHeight,
                line.quantity(PcbWords.ROUNDNESS).orElse(0.0), path(line), line.word(PcbWords.FACE).orElse(""), line.word(PcbWords.OPENS).orElse(""),
                line.quantity(PcbWords.MASK_MARGIN).orElse(null), line.quantity(PcbWords.PASTE_MARGIN).orElse(null));
    }

    private static String path(ClauseLineNode line) {
        return line.endingValue().map(value -> ((PathTextNode) value).path().toSource()).orElse("");
    }
}
