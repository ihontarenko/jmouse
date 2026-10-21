package org.jmouse.parallax.script.writer;

import org.jmouse.parallax.script.Block;
import org.jmouse.parallax.script.Block.Equation;
import org.jmouse.parallax.script.Block.Parameter;
import org.jmouse.parallax.script.Block.ParameterFlag;
import org.jmouse.parallax.script.Block.Pin;
import org.jmouse.parallax.script.Block.PinDirection;
import org.jmouse.parallax.script.SourceText;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * The declaration blocks every computable kind shares, written as the reader's line parsers read them: parameters,
 * inputs, outputs, state, equations, heat, image, display, measure, leads, pins, nodes — and a {@code type}.
 */
final class DeclarationWriter {

    /** The flags written as one word after a parameter's value, in the order they are written. */
    private static final List<ParameterFlag> WORD_FLAGS = List.of(ParameterFlag.LOG, ParameterFlag.INTEGER,
            ParameterFlag.EXACT, ParameterFlag.ANIMATE, ParameterFlag.LIVE);

    private DeclarationWriter() {
    }

    // ─── parameters, inputs, outputs, state ─────────────────────────────────────────────────────────

    /** {@code name = value [unit] [in minimum .. maximum | in [a, b]] [step s] [flags] ["description"]}. */
    static String parameter(Parameter parameter) {
        StringBuilder line = new StringBuilder(parameter.name()).append(" = ");
        if (parameter.has(ParameterFlag.TEXT)) {
            line.append(parameter.value().text());
            if (!parameter.values().isEmpty()) {
                line.append(" in ").append(listed(parameter.values()));
            }
            return line.append(Values.described(parameter.description())).toString();
        }
        line.append(Values.written(parameter.value(), parameter.quantity()));
        if (!parameter.values().isEmpty()) {
            line.append(" in ").append(listed(parameter.values()));
        } else {
            parameter.range().ifPresent(range -> line.append(" in ").append(Values.range(range)));
        }
        parameter.step().ifPresent(step -> line.append(" step ").append(step.text()));
        for (ParameterFlag flag : WORD_FLAGS) {
            if (parameter.has(flag)) {
                line.append(' ').append(flag.name().toLowerCase(Locale.ROOT));
            }
        }
        if (parameter.has(ParameterFlag.PER_UNIT)) {
            line.append(" per unit");
        }
        return line.append(Values.described(parameter.description())).toString();
    }

    private static String listed(List<SourceText> values) {
        return "[" + String.join(", ", values.stream().map(SourceText::text).toList()) + "]";
    }

    /** {@code x in -3 .. 3}, {@code (x, y) in -1 .. 1}, {@code (z) in …}, then step, integer, periodic, description. */
    static String input(Block.Input input) {
        boolean parenthesised = input.names().size() > 1 || input.complex();
        StringBuilder line = new StringBuilder(parenthesised ? "(" + Values.names(input.names()) + ")"
                : input.names().getFirst());
        input.range().ifPresent(range -> line.append(" in ").append(Values.range(range)));
        input.step().ifPresent(step -> line.append(" step ").append(step.text()));
        if (input.integer()) {
            line.append(" integer");
        }
        if (input.periodic()) {
            line.append(" periodic");
        }
        return line.append(Values.described(input.description())).toString();
    }

    /** {@code name [: type [glued g]] (= value | where equation) ["description"]}. */
    static String output(Block.Output output) {
        StringBuilder line = new StringBuilder(output.name());
        output.type().ifPresent(type -> line.append(" : ").append(type.name().toLowerCase(Locale.ROOT)));
        output.gluing().ifPresent(gluing -> line.append(" glued ").append(gluing));
        if (!output.components().isEmpty()) {
            line.append(" = ").append(Values.tuple(output.components()));
        }
        output.equation().ifPresent(equation -> line.append(" where ").append(equation.text()));
        return line.append(Values.described(output.description())).toString();
    }

    /** {@code name[length] = initial continuous|discrete [in minimum .. maximum] ["description"]}. */
    static String state(Block.StateVariable variable) {
        StringBuilder line = new StringBuilder(variable.name());
        if (variable.length() > 0) {
            line.append('[').append(variable.length()).append(']');
        }
        line.append(" = ").append(variable.initial().text()).append(variable.discrete() ? " discrete" : " continuous");
        variable.range().ifPresent(range -> line.append(" in ").append(Values.range(range)));
        return line.append(Values.described(variable.description())).toString();
    }

    // ─── equations ──────────────────────────────────────────────────────────────────────────────────

    /** One equation in the form it was read in. */
    static String equation(Equation equation) {
        String right = equation.expression().text();
        return switch (equation.form()) {
            case DERIVATIVE -> "d(" + equation.subject() + ")/dt = " + right;
            case NEXT -> equation.subject() + "_next = " + right;
            case START -> equation.subject() + "_start = " + right;
            case CURRENT -> "current " + equation.positive() + " -> " + equation.negative() + " = " + right;
            case VOLTAGE -> "voltage " + equation.positive() + " -> " + equation.negative() + " = " + right;
            case IMPLICIT -> "0 = " + right;
            case LOCAL -> "let " + equation.subject() + (equation.type() == null ? "" : " : " + equation.type().name())
                    + " = " + right;
        };
    }

    /** {@code type name = known * types / joined}. */
    static String type(Block.TypeDefinition type) {
        return "type " + type.type() + " = " + type.definition().text();
    }

    static void heat(Block.Heat heat, SourceLines lines) {
        lines.block("heat", inner -> {
            heat.lines().forEach(equation -> inner.line(equation(equation)));
            inner.line("temperature = " + heat.temperature().text());
        });
    }

    /** {@code over}, a {@code start} for each state variable, every {@code stop when}, {@code hit when}, {@code value}. */
    static void image(Block.Image image, SourceLines lines) {
        lines.block("image", inner -> {
            inner.line("over " + image.horizontal() + " in " + Values.range(image.horizontalRange()) + ", "
                    + image.vertical() + " in " + Values.range(image.verticalRange()));
            image.starts().forEach((variable, value) -> inner.line("start " + variable + " = " + value.text()));
            image.stops().forEach(condition -> inner.line("stop when " + condition.text()));
            inner.line("hit when " + image.crossing().text() + " crosses 0"
                    + image.where().map(where -> " where " + where.text()).orElse(""));
            inner.line("value = " + image.value().text());
        });
    }

    // ─── what a part shows and measures ─────────────────────────────────────────────────────────────

    /**
     * {@code name = expression [unit U] ["caption"]}; a row of figures the reader spread over {@code cell_0 … cell_31}
     * is written back as the one line it was, {@code cell[32] = shown}.
     */
    static void display(Block.Display display, SourceLines lines) {
        lines.block("display", inner -> {
            List<String> names = new ArrayList<>(display.figures().keySet());
            int index = 0;
            while (index < names.size()) {
                String name = names.get(index);
                int row = rowLength(display, names, index);
                if (row > 1) {
                    String base = name.substring(0, name.length() - 2);
                    String shown = display.figures().get(name).text();
                    inner.line(base + "[" + row + "] = " + shown.substring(0, shown.length() - 2)
                            + Values.described(display.captions().get(name)));
                    index += row;
                    continue;
                }
                StringBuilder line = new StringBuilder(name).append(" = ").append(display.figures().get(name).text());
                if (display.units().containsKey(name)) {
                    line.append(" unit ").append(display.units().get(name));
                }
                inner.line(line.append(Values.described(display.captions().get(name))).toString());
                index++;
            }
        });
    }

    /**
     * How many figures from {@code from} on make one row: {@code name_0 = shown_0}, {@code name_1 = shown_1} …, each
     * without a unit and with the same caption; 0 when the figure there opens none.
     */
    private static int rowLength(Block.Display display, List<String> names, int from) {
        String first = names.get(from);
        String firstShown = display.figures().get(first).text();
        if (!first.endsWith("_0") || !firstShown.endsWith("_0")) {
            return 0;
        }
        String base = first.substring(0, first.length() - 2);
        String shown = firstShown.substring(0, firstShown.length() - 2);
        String caption = display.captions().get(first);
        int length = 0;
        while (from + length < names.size()) {
            String name = names.get(from + length);
            if (!name.equals(base + "_" + length)
                    || !display.figures().get(name).text().equals(shown + "_" + length)
                    || display.units().containsKey(name)
                    || !Objects.equals(display.captions().get(name), caption)) {
                break;
            }
            length++;
        }
        return length;
    }

    /** {@code name = expression unit U [signal]}. */
    static void measure(Block.Measure measure, SourceLines lines) {
        lines.block("measure", inner -> measure.figures().forEach((name, figure) -> inner.line(name + " = "
                + figure.expression().text() + " unit " + figure.unit() + (figure.signal() ? " signal" : ""))));
    }

    /** {@code pin colour [stripes colour]}. */
    static void leads(Block.Leads leads, SourceLines lines) {
        lines.block("leads", inner -> leads.looks().forEach((pin, look) -> inner.line(pin + " " + look.colour()
                + (look.stripes() == null ? "" : " stripes " + look.stripes()))));
    }

    // ─── pins and nodes ─────────────────────────────────────────────────────────────────────────────

    /**
     * Every pin on its line — {@code out analog output 1 7 "output"} —, and the bits of a bus together on the one line
     * they were read from: {@code q[0..7] digital output 15, 1, 2, 3, 4, 5, 6, 7}, the range in the order its bits stand.
     */
    static void pins(Block.Pins pins, SourceLines lines) {
        Map<String, Block.Bus> busOfPin = new HashMap<>();
        pins.buses().forEach(bus -> bus.pins().forEach(pin -> busOfPin.put(pin, bus)));
        lines.block("pins", inner -> {
            Set<String> written = new HashSet<>();
            List<Pin> all = pins.pins();
            for (int index = 0; index < all.size(); index++) {
                Pin pin = all.get(index);
                Block.Bus bus = busOfPin.get(pin.name());
                if (bus == null) {
                    inner.line(pin.name() + pinWords(pin, String.join(" ",
                            pin.numbers().stream().map(String::valueOf).toList())));
                } else if (written.add(bus.name())) {
                    List<Pin> bits = all.subList(index, index + bus.pins().size());
                    List<String> numbers = bits.stream().flatMap(bit -> bit.numbers().stream()).map(String::valueOf).toList();
                    inner.line(bus.name() + "[" + bitOf(bus, bits.getFirst()) + ".." + bitOf(bus, bits.getLast()) + "]"
                            + pinWords(pin, String.join(", ", numbers)));
                }
            }
        });
    }

    private static String bitOf(Block.Bus bus, Pin pin) {
        return pin.name().substring(bus.name().length() + 1);
    }

    /** What follows a pin's name: its nature, its direction when it is not an input, its numbers, attributes, description. */
    private static String pinWords(Pin pin, String numbers) {
        StringBuilder words = new StringBuilder(" ").append(pin.nature().name().toLowerCase(Locale.ROOT));
        if (pin.direction() != PinDirection.INPUT) {
            words.append(' ').append(pin.direction().name().toLowerCase(Locale.ROOT));
        }
        if (!numbers.isEmpty()) {
            words.append(' ').append(numbers);
        }
        pin.attributes().forEach(attribute -> words.append(' ').append(attribute));
        return words.append(Values.described(pin.description())).toString();
    }

    /** {@code [digital] name ["description"]}. */
    static String node(Block.InternalNode node) {
        return (node.digital() ? "digital " : "") + node.name() + Values.described(node.description());
    }
}
