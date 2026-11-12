package org.jmouse.cad.plugin.drawings;

import org.jmouse.strokefont.StrokeText;

import java.util.List;

/**
 * One sheet of a drawing, whatever file it ends up in: its marks in millimetres on paper, from the lower left corner, Y up.
 *
 * @param name  the word its file is called by: {@code fabrication}
 * @param title what it is headed with
 */
public record Sheet(String name, String title, SheetSettings.Paper paper, String scale, SheetPurpose purpose, List<Mark> marks) {

    public Sheet {
        marks = List.copyOf(marks);
    }

    /** How much weight words stand with: a heading's, or a text's. */
    public enum Weight {
        REGULAR,
        BOLD
    }

    /** A place on the paper. */
    public record Point(double x, double y) {
    }

    /** What a sheet is drawn of. */
    public sealed interface Mark {

        SheetLayer layer();

        /** Straight pieces through points, with a pen in millimetres. */
        record Line(SheetLayer layer, List<Point> points, boolean closed, double pen) implements Mark {
            public Line {
                points = List.copyOf(points);
            }
        }

        /** An area filled: its rings, of which any inside another is a hole. */
        record Area(SheetLayer layer, List<List<Point>> rings) implements Mark {
            public Area {
                rings = rings.stream().map(List::copyOf).toList();
            }
        }

        record Circle(SheetLayer layer, Point centre, double radius, double pen) implements Mark {
        }

        /** A mark in a colour of its own, in what shows colours: a schematic keeps its symbols' and its nets'. */
        record Inked(Mark mark, String colour) implements Mark {

            @Override
            public SheetLayer layer() {
                return mark.layer();
            }
        }

        /**
         * One line of words.
         *
         * @param height   how high a capital stands
         * @param rotation degrees anticlockwise, about {@code at}
         */
        record Words(SheetLayer layer, String text, Point at, double height, double rotation, StrokeText.Anchor anchor,
                     StrokeText.Baseline baseline, Weight weight) implements Mark {

            /** Words of a text's weight. */
            public Words(SheetLayer layer, String text, Point at, double height, double rotation, StrokeText.Anchor anchor, StrokeText.Baseline baseline) {
                this(layer, text, at, height, rotation, anchor, baseline, Weight.REGULAR);
            }
        }
    }
}
