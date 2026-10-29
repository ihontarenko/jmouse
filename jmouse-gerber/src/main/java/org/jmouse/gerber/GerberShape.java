package org.jmouse.gerber;

import org.jmouse.el.writer.SourceLayout;
import org.jmouse.el.writer.SourceWritable;
import org.jmouse.el.writer.SourceWriter;
import org.jmouse.el.writer.TextSourceWriter;

import java.math.BigDecimal;
import java.util.List;

/**
 * What an aperture is: one of the four standard ones — a circle, a rectangle, an obround (a rectangle with round ends),
 * a regular polygon —, each with an optional round hole, or a macro the file defines with {@code %AM}, by its name and
 * parameters. A hole of 0 is none. It writes itself as {@code %ADD} holds it after the code: {@code C,0.25},
 * {@code R,1.5X0.8}.
 */
public sealed interface GerberShape extends SourceWritable {

    /** What separates a template's name from its parameters. */
    String NAME_SEPARATOR = ",";
    /** What separates its parameters. */
    String PARAMETER_SEPARATOR = "X";

    /** The template as written: what tells two apertures apart. */
    default String template() {
        return TextSourceWriter.render(this, SourceLayout.GLUED);
    }

    record Circle(double diameter, double hole) implements GerberShape {

        public Circle(double diameter) {
            this(diameter, 0);
        }

        @Override
        public void writeSource(SourceWriter writer) {
            writeStandard(writer, "C", diameter, hole);
        }
    }

    record Rectangle(double width, double height, double hole) implements GerberShape {

        public Rectangle(double width, double height) {
            this(width, height, 0);
        }

        @Override
        public void writeSource(SourceWriter writer) {
            writeStandard(writer, "R", width, height, hole);
        }
    }

    record Obround(double width, double height, double hole) implements GerberShape {

        public Obround(double width, double height) {
            this(width, height, 0);
        }

        @Override
        public void writeSource(SourceWriter writer) {
            writeStandard(writer, "O", width, height, hole);
        }
    }

    /** @param rotation degrees */
    record Polygon(double diameter, int vertices, double rotation, double hole) implements GerberShape {

        public Polygon(double diameter, int vertices, double rotation) {
            this(diameter, vertices, rotation, 0);
        }

        @Override
        public void writeSource(SourceWriter writer) {
            writer.word("P").join(NAME_SEPARATOR).number(rounded(diameter)).join(PARAMETER_SEPARATOR).number(vertices);
            // A hole is the fourth parameter: the rotation before it is written even when it is none.
            if (rotation != 0 || hole != 0) {
                writer.join(PARAMETER_SEPARATOR).number(rounded(rotation));
            }
            if (hole != 0) {
                writer.join(PARAMETER_SEPARATOR).number(rounded(hole));
            }
        }
    }

    /** @param parameters as written, each a number or an expression of the macro's language */
    record Macro(String name, List<String> parameters) implements GerberShape {

        public Macro {
            parameters = List.copyOf(parameters);
        }

        @Override
        public void writeSource(SourceWriter writer) {
            writer.word(name);
            for (int index = 0; index < parameters.size(); index++) {
                writer.join(index == 0 ? NAME_SEPARATOR : PARAMETER_SEPARATOR).word(parameters.get(index));
            }
        }
    }

    /** A template as {@code %ADD} holds it after its code — {@code C,0.25}, {@code R,1.5X0.8X0.4}, {@code THERMAL80} —, read. */
    static GerberShape of(String template) {
        int comma = template.indexOf(NAME_SEPARATOR);
        String name = comma < 0 ? template : template.substring(0, comma);
        List<String> parameters = comma < 0 ? List.of() : List.of(template.substring(comma + 1).split(PARAMETER_SEPARATOR, -1));
        try {
            return switch (name) {
                case "C" -> new Circle(number(parameters, 0), number(parameters, 1));
                case "R" -> new Rectangle(number(parameters, 0), number(parameters, 1), number(parameters, 2));
                case "O" -> new Obround(number(parameters, 0), number(parameters, 1), number(parameters, 2));
                case "P" -> new Polygon(number(parameters, 0), (int) number(parameters, 1), number(parameters, 2),
                        number(parameters, 3));
                default -> new Macro(name, parameters);
            };
        } catch (NumberFormatException notNumber) {
            return new Macro(name, parameters);
        }
    }

    private static double number(List<String> parameters, int index) {
        return index < parameters.size() ? Double.parseDouble(parameters.get(index)) : 0;
    }

    private static BigDecimal rounded(double value) {
        return GerberNumbers.rounded(value, GerberNumbers.APERTURE_DECIMALS);
    }

    /** A standard aperture: its letter, its sizes; a trailing hole of none is not written. */
    private static void writeStandard(SourceWriter writer, String letter, double... values) {
        int written = values[values.length - 1] == 0 ? values.length - 1 : values.length;
        writer.word(letter);
        for (int index = 0; index < written; index++) {
            writer.join(index == 0 ? NAME_SEPARATOR : PARAMETER_SEPARATOR).number(rounded(values[index]));
        }
    }
}
