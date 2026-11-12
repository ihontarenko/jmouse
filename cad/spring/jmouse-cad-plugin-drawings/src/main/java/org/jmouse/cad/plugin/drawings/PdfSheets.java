package org.jmouse.cad.plugin.drawings;

import org.jmouse.pdf.PdfCanvas;
import org.jmouse.pdf.PdfColour;
import org.jmouse.pdf.PdfDocument;
import org.jmouse.pdf.PdfFont;
import org.jmouse.strokefont.StrokeFont;
import org.jmouse.strokefont.StrokeText;

import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

/**
 * Sheets as one PDF, a page each. Words are set in the fonts found on the server, embedded, so they read the same anywhere and can
 * be searched; with none found, or for a word its font has no glyph for, they are drawn as strokes of the engine's own font.
 */
final class PdfSheets {

    private static final double SCALE = PdfDocument.POINTS_PER_MILLIMETRE;

    private final SheetSettings             settings;
    private final StrokeFont                strokes;
    private final Optional<SheetFonts.Faces> faces;

    PdfSheets(SheetSettings settings, StrokeFont strokes, Optional<SheetFonts.Faces> faces) {
        this.settings = settings;
        this.strokes = strokes;
        this.faces = faces;
    }

    byte[] write(String title, String producer, List<Sheet> sheets) {
        PdfDocument document = new PdfDocument(title, producer);
        faces.ifPresent(found -> document.embed(found.regular()).embed(found.bold()));
        for (Sheet sheet : sheets) {
            PdfCanvas canvas = document.pageInMillimetres(sheet.paper().width(), sheet.paper().height());
            sheet.marks().forEach(mark -> draw(canvas, mark));
        }
        return document.write();
    }

    private void draw(PdfCanvas canvas, Sheet.Mark mark) {
        canvas.colour(PdfColour.ofHex(mark instanceof Sheet.Mark.Inked inked ? inked.colour() : settings.penOf(mark.layer()).hex()));
        drawn(canvas, mark instanceof Sheet.Mark.Inked inked ? inked.mark() : mark);
    }

    private void drawn(PdfCanvas canvas, Sheet.Mark mark) {
        switch (mark) {
            case Sheet.Mark.Line line -> {
                canvas.lineWidth(line.pen() * SCALE);
                trace(canvas, line.points(), line.closed());
                canvas.stroke();
            }
            case Sheet.Mark.Area area -> {
                area.rings().forEach(ring -> trace(canvas, ring, true));
                canvas.fillEvenOdd();
            }
            case Sheet.Mark.Circle circle -> {
                canvas.lineWidth(circle.pen() * SCALE);
                trace(canvas, circleOf(circle), true);
                canvas.stroke();
            }
            case Sheet.Mark.Words words -> lettered(canvas, words);
            case Sheet.Mark.Inked inked -> drawn(canvas, inked.mark());
        }
    }

    private List<Sheet.Point> circleOf(Sheet.Mark.Circle circle) {
        int steps = settings.circleSteps();
        return IntStream.range(0, steps).mapToObj(step -> {
            double angle = 2 * Math.PI * step / steps;
            return new Sheet.Point(circle.centre().x() + circle.radius() * Math.cos(angle), circle.centre().y() + circle.radius() * Math.sin(angle));
        }).toList();
    }

    private void lettered(PdfCanvas canvas, Sheet.Mark.Words words) {
        Optional<PdfFont> font = faces.map(found -> words.weight() == Sheet.Weight.BOLD ? found.bold() : found.regular())
                .filter(candidate -> words.text().codePoints().allMatch(candidate::has));
        if (font.isPresent()) {
            set(canvas, words, font.get());
        } else {
            stroked(canvas, words);
        }
    }

    /** Words in a real font, moved from where they stand against their point to where their line starts on its baseline. */
    private static void set(PdfCanvas canvas, Sheet.Mark.Words words, PdfFont font) {
        double size = font.sizeForCapHeight(words.height());
        double along = switch (words.anchor()) {
            case START -> 0;
            case MIDDLE -> -font.widthOf(words.text(), size) / 2;
            case END -> -font.widthOf(words.text(), size);
        };
        double up = switch (words.baseline()) {
            case ALPHABETIC, BOTTOM -> 0;
            case MIDDLE -> -words.height() / 2;
            case TOP -> -words.height();
        };
        double angle = Math.toRadians(words.rotation());
        double x = words.at().x() + along * Math.cos(angle) - up * Math.sin(angle);
        double y = words.at().y() + along * Math.sin(angle) + up * Math.cos(angle);
        canvas.text(font, size * SCALE, x * SCALE, y * SCALE, words.rotation(), words.text());
    }

    /** The strokes of the words, which stand with Y down, turned about their point and set on a page with Y up. */
    private void stroked(PdfCanvas canvas, Sheet.Mark.Words words) {
        StrokeText laid = StrokeText.lay(strokes, words.text(), words.height(), words.anchor(), words.baseline(), settings.curveSteps());
        double angle = Math.toRadians(words.rotation());
        double cosine = Math.cos(angle);
        double sine = Math.sin(angle);
        canvas.lineWidth(words.height() * settings.text().stroke() * SCALE);
        boolean any = false;
        for (List<StrokeText.Point> stroke : laid.strokes()) {
            if (stroke.size() < 2) {
                continue;
            }
            any = true;
            boolean first = true;
            for (StrokeText.Point point : stroke) {
                double up = -point.y();
                double x = (words.at().x() + point.x() * cosine - up * sine) * SCALE;
                double y = (words.at().y() + point.x() * sine + up * cosine) * SCALE;
                if (first) {
                    canvas.moveTo(x, y);
                    first = false;
                } else {
                    canvas.lineTo(x, y);
                }
            }
        }
        if (any) {
            canvas.stroke();
        }
    }

    private static void trace(PdfCanvas canvas, List<Sheet.Point> points, boolean closed) {
        boolean first = true;
        for (Sheet.Point point : points) {
            if (first) {
                canvas.moveTo(point.x() * SCALE, point.y() * SCALE);
                first = false;
            } else {
                canvas.lineTo(point.x() * SCALE, point.y() * SCALE);
            }
        }
        if (closed) {
            canvas.close();
        }
    }
}
