package org.jmouse.cad.pcb.drawing;

import org.jmouse.cad.pcb.drawing.FootprintDrawing.Pad;
import org.jmouse.cad.pcb.drawing.FootprintDrawing.Outline;
import org.jmouse.cad.pcb.drawing.FootprintDrawing.Field;
import org.jmouse.cad.pcb.drawing.FootprintDrawing.Model;
import org.jmouse.cad.pcb.FootprintLines;
import org.jmouse.cad.pcb.PcbVocabulary;
import org.jmouse.el.grammar.ClauseLineNode;
import org.jmouse.svg.path.node.PathData;
import org.jmouse.svg.path.parser.PathParseException;
import org.jmouse.svg.path.parser.PathParser;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** A footprint written as its document's lines, built on the grammars that read them. {@link FootprintDocumentReading} reads them back. */
public final class FootprintDocumentWriting {

    private FootprintDocumentWriting() {
    }

    private static final PathParser PARSER = new PathParser();

    /**
     * The drawing as a footprint's lines — its drawing.pads(), its drawing.outlines(), its drawing.fields(), each in its order —, built on the grammars
     * that read them: what a footprint editor's drawing is written with. A field printed where a field is printed when
     * its line names no layer names none.
     *
     * @throws IllegalArgumentException when a path does not read as path data
     */
    static List<ClauseLineNode> written(FootprintDrawing drawing) {
        List<ClauseLineNode> lines = new ArrayList<>();
        for (Pad pad : drawing.pads()) {
            Optional<PathData> outline = pad.path() == null || pad.path().isBlank() ? Optional.empty()
                    : Optional.of(pathData("pad " + pad.number(), pad.path()));
            lines.add(FootprintLines.pad(new FootprintLines.Pad(pad.number(), pad.type(), pad.shape(), pad.x(), pad.y(),
                    pad.rotation(), pad.width(), pad.height(), pad.drillWidth(), pad.drillHeight(), pad.roundness(), outline,
                    pad.face(), pad.opens(), pad.maskMargin(), pad.pasteMargin()),
                    List.of()));
        }
        for (Outline outline : drawing.outlines()) {
            lines.add(FootprintLines.outline(new FootprintLines.Outline(outline.layer(), outline.width(), outline.filled(),
                    pathData("the " + outline.layer() + " outline", outline.path())), List.of()));
        }
        for (Field field : drawing.fields()) {
            String layer = PcbVocabulary.SILKSCREEN.equals(field.layer()) ? null : field.layer();
            lines.add(FootprintLines.field(new FootprintLines.Field(field.subject(), field.x(), field.y(), field.height(),
                    field.rotation(), layer, field.anchor()), List.of()));
        }
        for (Model model : drawing.models()) {
            lines.add(FootprintLines.model(new FootprintLines.Model(model.reference(), axes(model.offset()), axes(model.rotation()), axes(model.scale()),
                    model.hidden()), List.of()));
        }
        return lines;
    }

    private static FootprintLines.Axes axes(List<Double> values) {
        return new FootprintLines.Axes(values.get(0), values.get(1), values.get(2));
    }

    private static PathData pathData(String origin, String written) {
        try {
            return PARSER.parse(origin, written == null ? "" : written);
        } catch (PathParseException refused) {
            throw new IllegalArgumentException("the path data of " + origin + " does not read: " + refused.detail());
        }
    }
}
