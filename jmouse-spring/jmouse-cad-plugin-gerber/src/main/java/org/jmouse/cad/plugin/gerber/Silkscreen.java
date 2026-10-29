package org.jmouse.cad.plugin.gerber;

import org.jmouse.cad.board.drawing.BoardDrawing;
import org.jmouse.cad.board.drawing.FootprintDrawing;
import org.jmouse.cad.board.geometry.PlacedDesignator;
import org.jmouse.gerber.GerberShape;
import org.jmouse.gerber.compose.GerberComposer;
import org.jmouse.strokefont.StrokeFont;
import org.jmouse.strokefont.StrokeText;

import java.util.List;

/**
 * One side's silkscreen as Gerber X2: its parts' silkscreen outlines, each part's designator lettered where the board
 * shows it, and the board's own words on that side — all in the stroke font, read from above on the top and mirrored on
 * the bottom, as the board is seen from its own side.
 */
final class Silkscreen {

    private static final String LEGEND = "Other,Legend";

    private final StrokeFont              font;
    private final PlacedDesignator.Rules  designators;
    private final double                  lineWidth;
    private final double                  letterStroke;
    private final int                     curveSteps;

    /**
     * @param lineWidth    the width an outline is drawn with when it names none
     * @param letterStroke how thick a letter's stroke is, as a share of its height
     */
    Silkscreen(StrokeFont font, PlacedDesignator.Rules designators, double lineWidth, double letterStroke, int curveSteps) {
        this.font = font;
        this.designators = designators;
        this.lineWidth = lineWidth;
        this.letterStroke = letterStroke;
        this.curveSteps = curveSteps;
    }

    /** The side's file: {@code Legend,Top} or {@code Legend,Bot}. */
    String write(BoardFabrication fabrication, String side, String word) {
        GerberComposer legend = BoardGerber.file("Legend," + word, true);
        for (BoardFabrication.SeatedPart part : fabrication.parts()) {
            if (!part.placement().side().equals(side)) {
                continue;
            }
            for (FootprintDrawing.Outline outline : part.drawing().map(FootprintDrawing::outlines).orElse(List.of())) {
                if ("silkscreen".equals(outline.layer())) {
                    BoardGerber.outline(legend, part.placement(), outline, outline.width() > 0 ? outline.width() : lineWidth, LEGEND,
                            curveSteps);
                }
            }
            PlacedDesignator standing = PlacedDesignator.of(part.placement(), part.drawing(), designators);
            letter(legend, part.designator(), standing.x(), standing.y(), standing.height(), standing.rotation(), side);
        }
        for (BoardDrawing.Text words : fabrication.board().texts()) {
            if (words.layer().equals(side + "-silkscreen")) {
                letter(legend, words.text(), words.x(), words.y(), words.size(), words.rotation(), side);
            }
        }
        return legend.document().toSource();
    }


    /** Words centred on a point and turned counter-clockwise; mirrored on the bottom, as the bottom is seen from below. */
    private void letter(GerberComposer composer, String words, double x, double y, double height, double rotation, String side) {
        StrokeText lettered = StrokeText.lay(font, words, height, StrokeText.Anchor.MIDDLE, StrokeText.Baseline.MIDDLE, curveSteps);
        int pen = composer.aperture(new GerberShape.Circle(height * letterStroke), LEGEND);
        double angle = Math.toRadians(rotation);
        double mirror = BoardDrawing.BOTTOM.equals(side) ? -1 : 1;
        for (List<StrokeText.Point> stroke : lettered.strokes()) {
            if (stroke.size() < 2) {
                continue;
            }
            composer.line(pen, stroke.stream().map(point -> {
                double across = point.x() * mirror;
                return BoardGerber.out(x + across * Math.cos(angle) + point.y() * Math.sin(angle),
                        y - across * Math.sin(angle) + point.y() * Math.cos(angle));
            }).toList());
        }
    }
}
