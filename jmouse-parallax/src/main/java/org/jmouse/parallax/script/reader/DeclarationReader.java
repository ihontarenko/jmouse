package org.jmouse.parallax.script.reader;

import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.Token;
import org.jmouse.parallax.script.Block;
import org.jmouse.parallax.script.Block.Equation;
import org.jmouse.parallax.script.Block.EquationForm;
import org.jmouse.parallax.script.Block.Input;
import org.jmouse.parallax.script.Block.InternalNode;
import org.jmouse.parallax.script.Block.Output;
import org.jmouse.parallax.script.Block.OutputType;
import org.jmouse.parallax.script.Block.Parameter;
import org.jmouse.parallax.script.Block.ParameterFlag;
import org.jmouse.parallax.script.Block.Pin;
import org.jmouse.parallax.script.Block.PinNature;
import org.jmouse.parallax.script.Block.Range;
import org.jmouse.parallax.script.Block.StateVariable;
import org.jmouse.parallax.script.SourceText;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/** The declaration blocks every computable kind shares: parameters, inputs, outputs, state, equations, pins, nodes. */
final class DeclarationReader {

    private static final List<String> PARAMETER_MARKERS = List.of("in", "step", "log", "integer", "animate", "live", "per", "exact");

    /** The parameter flags written as one word, each its {@link ParameterFlag} in lower case; {@code per unit} is two. */
    private static final List<String> SINGLE_WORD_FLAGS = List.of("log", "integer", "animate", "live", "exact");

    private final Reading reading;

    DeclarationReader(Reading reading) {
        this.reading = reading;
    }

    // ─── parameters ─────────────────────────────────────────────────────────────────────────────────

    Block.Parameters parameters() {
        return new Block.Parameters(reading.blockLines("parameters").stream().map(this::parameter).toList());
    }

    /** {@code name = value [unit] [in a .. b] [step s] [log] [integer] [animate] [live] [per unit] ["description"]}. */
    Parameter parameter(Reading.Line line) {
        if (line.size() < 3 || !line.value(1).equals("=")) {
            throw line.refusal("a parameter is 'name = value [unit] [in minimum .. maximum] [step s] [flags] [\"description\"]'");
        }
        String name = line.value(0);
        int valueEnd = firstMarker(line, 2);
        Values.Written value = Values.written(line, 2, valueEnd);
        Optional<Range> range = Optional.empty();
        Optional<SourceText> step = Optional.empty();
        Set<ParameterFlag> flags = EnumSet.noneOf(ParameterFlag.class);
        String description = "";
        List<SourceText> listed = List.of();
        int index = valueEnd;
        while (index < line.size()) {
            String word = line.value(index);
            if (line.isString(index)) {
                description = reading.literal(line.get(index));
                index++;
            } else if (word.equals("in")) {
                int end = firstMarker(line, index + 1, "in");
                if (line.value(index + 1).equals("[") && line.value(end - 1).equals("]")) {
                    // `in [1, 3, 7, 19, 49]` — a jME list: the values a slider stops at, from the first to the last.
                    listed = new ArrayList<>();
                    int start = index + 2;
                    for (int comma : line.commas(index + 2, end - 1)) {
                        listed.add(line.text(start, comma));
                        start = comma + 1;
                    }
                    listed.add(line.text(start, end - 1));
                    if (listed.size() < 2) {
                        throw line.refusal(index, "a slider over listed values lists at least two: 'in [1, 3, 7]'");
                    }
                    range = Optional.of(new Range(listed.getFirst(), listed.getLast()));
                } else {
                    range = Optional.of(Values.range(line, index + 1, end));
                }
                index = end;
            } else if (word.equals("step")) {
                int end = firstMarker(line, index + 1, "step");
                step = Optional.of(line.text(index + 1, end));
                index = end;
            } else if (word.equals("per") && line.value(index + 1).equals("unit")) {
                flags.add(ParameterFlag.PER_UNIT);
                index += 2;
            } else if (SINGLE_WORD_FLAGS.contains(word)) {
                flags.add(ParameterFlag.valueOf(word.toUpperCase(Locale.ROOT)));
                index++;
            } else {
                throw line.refusal(index, "'" + word + "' is not part of a parameter: expected in, step, log, integer, "
                        + "exact, animate, live, per unit or a description");
            }
        }
        return new Parameter(name, value.expression(), value.quantity(), range, step, flags, description, listed);
    }

    /** The first parameter marker at depth zero after {@code from}, or the description, or the end of the line. */
    private int firstMarker(Reading.Line line, int from, String... besides) {
        for (int index = from; index < line.size(); index++) {
            if (line.isString(index)) {
                return index;
            }
            String word = line.value(index);
            if (PARAMETER_MARKERS.contains(word) && !List.of(besides).contains(word) && !line.value(index + 1).equals("(")
                    && depthAt(line, from, index) == 0) {
                return index;
            }
        }
        return line.size();
    }

    private static int depthAt(Reading.Line line, int from, int at) {
        int depth = 0;
        for (int index = from; index < at; index++) {
            String value = line.value(index);
            if (value.equals("(") || value.equals("[")) {
                depth++;
            } else if (value.equals(")") || value.equals("]")) {
                depth--;
            }
        }
        return depth;
    }

    // ─── inputs ─────────────────────────────────────────────────────────────────────────────────────

    Block.Inputs inputs() {
        return new Block.Inputs(reading.blockLines("inputs").stream().map(this::input).toList());
    }

    /** {@code x in -3 .. 3}, {@code n in 0 .. 500 integer}, {@code (z) in -2 - 2 i .. 2 + 2 i}, {@code (x, y) in -1 .. 1}. */
    private Input input(Reading.Line line) {
        List<String> names = new ArrayList<>();
        int index;
        boolean parenthesised = line.value(0).equals("(");
        if (parenthesised) {
            int close = line.closing(0);
            for (int at = 1; at < close; at++) {
                if (!line.value(at).equals(",")) {
                    names.add(line.value(at));
                }
            }
            index = close + 1;
        } else {
            names.add(line.value(0));
            index = 1;
        }
        Optional<Range> range = Optional.empty();
        Optional<SourceText> step = Optional.empty();
        boolean integer = false;
        boolean periodic = false;
        String description = "";
        while (index < line.size()) {
            String word = line.value(index);
            if (line.isString(index)) {
                description = reading.literal(line.get(index));
                index++;
            } else if (word.equals("in")) {
                int end = endOfInputPart(line, index + 1);
                range = Optional.of(Values.range(line, index + 1, end));
                index = end;
            } else if (word.equals("step")) {
                int end = endOfInputPart(line, index + 1);
                step = Optional.of(line.text(index + 1, end));
                index = end;
            } else if (word.equals("integer")) {
                integer = true;
                index++;
            } else if (word.equals("periodic")) {
                periodic = true;
                index++;
            } else {
                throw line.refusal(index, "'" + word + "' is not part of an input: expected in, step, integer, periodic or a description");
            }
        }
        return new Input(names, range, step, integer, periodic, parenthesised && names.size() == 1, description);
    }

    private static int endOfInputPart(Reading.Line line, int from) {
        int index = line.indexOf(from, "step", "integer", "periodic");
        for (int at = from; at < line.size(); at++) {
            if (line.isString(at)) {
                return index < 0 ? at : Math.min(index, at);
            }
        }
        return index < 0 ? line.size() : index;
    }

    // ─── outputs ────────────────────────────────────────────────────────────────────────────────────

    Block.Outputs outputs() {
        return new Block.Outputs(reading.blockLines("outputs").stream().map(this::output).toList());
    }

    /**
     * {@code z : surface = A sin(k x)}, {@code trace : curve = (sin(t), cos(t))}, {@code boundary : surface where …},
     * {@code estimate : series "…"}, {@code mean = mu}.
     */
    private Output output(Reading.Line line) {
        String name = line.value(0);
        int index = 1;
        Optional<OutputType> type = Optional.empty();
        Optional<String> gluing = Optional.empty();
        if (line.value(index).equals(":")) {
            String word = line.value(index + 1);
            type = Optional.of(outputType(line, index + 1, word));
            index += 2;
            if (line.value(index).equals("glued")) {
                gluing = Optional.of(line.value(index + 1));
                index += 2;
            }
        }
        int descriptionAt = line.size();
        String description = "";
        if (line.isString(line.size() - 1) && line.size() - 1 >= index) {
            descriptionAt = line.size() - 1;
            description = reading.literal(line.get(descriptionAt));
        }
        if (index >= descriptionAt) {
            if (type.isEmpty()) {
                throw line.refusal("output '" + name + "' needs a type ('" + name + " : series') or a value");
            }
            return new Output(name, type, gluing, List.of(), Optional.empty(), description);
        }
        String word = line.value(index);
        if (word.equals("=")) {
            return new Output(name, type, gluing, Values.tuple(line, index + 1, descriptionAt), Optional.empty(), description);
        }
        if (word.equals("where")) {
            return new Output(name, type, gluing, List.of(), Optional.of(line.text(index + 1, descriptionAt)), description);
        }
        throw line.refusal(index, "an output is 'name [: type] = expression', 'name : type where equation' or 'name : type'");
    }

    private static OutputType outputType(Reading.Line line, int index, String word) {
        try {
            return OutputType.valueOf(word.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException unknown) {
            throw line.refusal(index, "'" + word + "' is not an output type: " + Arrays.stream(OutputType.values())
                    .map(type -> type.name().toLowerCase(Locale.ROOT)).collect(Collectors.joining(", ")));
        }
    }

    // ─── state ──────────────────────────────────────────────────────────────────────────────────────

    Block.State state() {
        return new Block.State(reading.blockLines("state").stream().map(this::stateVariable).toList());
    }

    /** {@code x = 1 continuous "…"}, {@code stored = X discrete}. */
    private StateVariable stateVariable(Reading.Line line) {
        int nature = line.indexOf(2, "continuous", "discrete");
        if (line.size() < 4 || !line.value(1).equals("=") || nature < 0) {
            throw line.refusal("a state is 'name = initial continuous|discrete [\"description\"]'");
        }
        boolean described = line.isString(line.size() - 1);
        String description = described ? reading.literal(line.get(line.size() - 1)) : "";
        int end = described ? line.size() - 1 : line.size();
        Optional<Range> range = Optional.empty();
        if (nature + 1 < end) {
            if (!line.value(nature + 1).equals("in")) {
                throw line.refusal(nature + 1, "after continuous or discrete comes 'in minimum .. maximum' or a description");
            }
            range = Optional.of(Values.range(line, nature + 2, end));
        }
        return new StateVariable(line.value(0), line.text(2, nature), line.value(nature).equals("discrete"), range, description);
    }

    // ─── equations ──────────────────────────────────────────────────────────────────────────────────

    /**
     * {@code display { level = terminal_one - terminal_two;  charge = 100 * (terminal_one - terminal_two) / voltage_rating unit % }}:
     * a name, then the expression it shows — and, with {@code unit}, a figure written beside the part as well as drawn,
     * with an optional caption in quotes saying what it is of: {@code charge = … unit % "of the rated voltage"}.
     */
    Block.Display display() {
        Map<String, SourceText> figures = new LinkedHashMap<>();
        Map<String, String> units = new LinkedHashMap<>();
        Map<String, String> captions = new LinkedHashMap<>();
        for (Reading.Line line : reading.blockLines("display")) {
            int unit = line.indexOf(2, "unit");
            boolean captioned = unit >= 0 && unit == line.size() - 3 && line.isString(line.size() - 1);
            if (line.size() < 3 || !line.value(1).equals("=") || (unit >= 0 && unit != line.size() - (captioned ? 3 : 2))) {
                throw line.refusal("a display line is 'name = expression [unit U [\"caption\"]]': "
                        + "'charge = 100 * (terminal_one - terminal_two) / voltage_rating unit % \"of the rated voltage\"'");
            }
            if (figures.containsKey(line.value(0))) {
                throw line.refusal("'" + line.value(0) + "' is displayed twice");
            }
            figures.put(line.value(0), line.text(2, unit >= 0 ? unit : line.size()));
            if (unit >= 0) {
                units.put(line.value(0), line.value(unit + 1));
            }
            if (captioned) {
                captions.put(line.value(0), line.words(line.size() - 1, line.size()).getFirst());
            }
        }
        return new Block.Display(figures, units, captions);
    }

    /**
     * {@code measure { reading = plus - minus unit V signal }}: a name, then the expression it reads, then {@code unit} and
     * what that comes out in, and {@code signal} when it is a signal a bench reads as DC, AC or RMS.
     */
    Block.Measure measure() {
        Map<String, Block.MeasureFigure> figures = new LinkedHashMap<>();
        Token open = reading.cursor.current();
        for (Reading.Line line : reading.blockLines("measure")) {
            int unit = line.indexOf(2, "unit");
            boolean signal = line.value(line.size() - 1).equals("signal");
            if (line.size() < 3 || !line.value(1).equals("=") || unit < 3 || unit != line.size() - (signal ? 3 : 2)) {
                throw line.refusal("a measure line is 'name = expression unit U [signal]': 'reading = plus - minus unit V signal'");
            }
            if (figures.containsKey(line.value(0))) {
                throw line.refusal("'" + line.value(0) + "' is measured twice");
            }
            figures.put(line.value(0), new Block.MeasureFigure(line.text(2, unit), line.value(unit + 1), signal));
        }
        if (figures.isEmpty()) {
            throw reading.refusal(open, "a measure block names what the instrument reads: 'reading = plus - minus unit V'");
        }
        return new Block.Measure(figures);
    }

    /** {@code leads { plus red stripes black }}: a pin, its lead's colour, and optionally {@code stripes} and theirs. */
    Block.Leads leads() {
        Map<String, Block.LeadLook> looks = new LinkedHashMap<>();
        for (Reading.Line line : reading.blockLines("leads")) {
            boolean striped = line.size() == 4 && line.value(2).equals("stripes");
            if (line.size() != 2 && !striped) {
                throw line.refusal("a lead is 'pin colour [stripes colour]': 'plus red stripes black'");
            }
            for (int at : striped ? new int[]{1, 3} : new int[]{1}) {
                if (!Block.LEAD_COLOURS.contains(line.value(at))) {
                    throw line.refusal(at, "a lead is " + String.join(", ", Block.LEAD_COLOURS) + ", not '" + line.value(at) + "'");
                }
            }
            if (looks.containsKey(line.value(0))) {
                throw line.refusal("'" + line.value(0) + "' has its lead described twice");
            }
            looks.put(line.value(0), new Block.LeadLook(line.value(1), striped ? line.value(3) : null));
        }
        return new Block.Leads(looks);
    }

    /**
     * {@code image { … }}, one statement a line: {@code over X in a .. b, Y in c .. d}, {@code start u = …} for every state
     * variable, {@code stop when …} (any number), {@code hit when … crosses 0 [where …]}, {@code value = …}.
     */
    Block.Image image() {
        String horizontal = null;
        String vertical = null;
        Block.Range horizontalRange = null;
        Block.Range verticalRange = null;
        Map<String, SourceText> starts = new LinkedHashMap<>();
        List<SourceText> stops = new ArrayList<>();
        SourceText crossing = null;
        Optional<SourceText> where = Optional.empty();
        SourceText value = null;
        Token open = reading.cursor.current();
        for (Reading.Line line : reading.blockLines("image")) {
            switch (line.value(0)) {
                case "over" -> {
                    int comma = line.indexOf(1, ",");
                    if (comma < 0 || !line.value(2).equals("in") || !line.value(comma + 2).equals("in")) {
                        throw line.refusal("the picture spans two axes: 'over X in a .. b, Y in c .. d'");
                    }
                    horizontal = line.value(1);
                    horizontalRange = Values.range(line, 3, comma);
                    vertical = line.value(comma + 1);
                    verticalRange = Values.range(line, comma + 3, line.size());
                }
                case "start" -> {
                    requireEquals(line, 2);
                    starts.put(line.value(1), line.text(3));
                }
                case "stop" -> {
                    if (!line.value(1).equals("when")) {
                        throw line.refusal(1, "a run ends 'stop when condition'");
                    }
                    stops.add(line.text(2));
                }
                case "hit" -> {
                    int crosses = line.indexOf(2, "crosses");
                    if (!line.value(1).equals("when") || crosses < 0 || crosses + 1 >= line.size() || !line.value(crosses + 1).equals("0")) {
                        throw line.refusal("a hit is 'hit when expression crosses 0 [where condition]'");
                    }
                    crossing = line.text(2, crosses);
                    int whereAt = line.indexOf(crosses + 2, "where");
                    where = whereAt < 0 ? Optional.empty() : Optional.of(line.text(whereAt + 1));
                }
                case "value" -> {
                    requireEquals(line, 1);
                    value = line.text(2);
                }
                default -> throw line.refusal("an image holds 'over', 'start', 'stop when', 'hit when' and 'value ='");
            }
        }
        if (horizontal == null || crossing == null || value == null || starts.isEmpty() || stops.isEmpty()) {
            throw reading.refusal(open, "an image needs 'over', a 'start' for every state variable, at least one "
                    + "'stop when', a 'hit when' and a 'value'");
        }
        return new Block.Image(horizontal, horizontalRange, vertical, verticalRange, starts, stops, crossing, where, value);
    }

    Block.Equations equations() {
        return new Block.Equations(reading.blockLines("equations").stream().map(this::equation).toList());
    }

    /**
     * {@code d(x)/dt = …}, {@code x_next = …}, {@code x_start = …}, {@code current a -> b = …} (a current from pin a to
     * pin b through the part), {@code voltage a -> b = …} (a branch holding pin a at that voltage above pin b),
     * {@code 0 = …}, {@code local name = …}.
     */
    Equation equation(Reading.Line line) {
        String head = line.value(0);
        if (head.equals("local")) {
            requireEquals(line, 2);
            return new Equation(EquationForm.LOCAL, line.value(1), null, null, line.text(3));
        }
        if (head.equals("d") && line.value(1).equals("(") && line.value(3).equals(")") && line.value(4).equals("/")) {
            if (!line.value(5).equals("dt")) {
                throw line.refusal(5, "a derivative is written d(x)/dt = …");
            }
            requireEquals(line, 6);
            return new Equation(EquationForm.DERIVATIVE, line.value(2), null, null, line.text(7));
        }
        if (head.equals("current") || head.equals("voltage")) {
            int equals = line.indexOf(1, "=");
            String[] pins = equals < 0 ? new String[0] : String.join("", line.words(1, equals)).split("->", -1);
            if (pins.length != 2 || !pins[0].matches("[A-Za-z_][A-Za-z0-9_]*") || !pins[1].matches("[A-Za-z_][A-Za-z0-9_]*")) {
                throw line.refusal(1, "a branch is written '" + head + " a -> b = …', from one pin to another");
            }
            requireEquals(line, equals);
            return new Equation(head.equals("current") ? EquationForm.CURRENT : EquationForm.VOLTAGE, null,
                    pins[0], pins[1], line.text(equals + 1));
        }
        if ((head.equals("I") || head.equals("V")) && line.value(1).equals("(")) {
            throw line.refusal("a branch is written '" + (head.equals("I") ? "current" : "voltage") + " a -> b = …', not "
                    + head + "(a, b) = …");
        }
        if (head.equals("0") && line.value(1).equals("=")) {
            return new Equation(EquationForm.IMPLICIT, null, null, null, line.text(2));
        }
        if (line.value(1).equals("=") && line.get(0).type() == BasicToken.T_IDENTIFIER) {
            if (head.endsWith("_next") && head.length() > 5) {
                return new Equation(EquationForm.NEXT, head.substring(0, head.length() - 5), null, null, line.text(2));
            }
            if (head.endsWith("_start") && head.length() > 6) {
                return new Equation(EquationForm.START, head.substring(0, head.length() - 6), null, null, line.text(2));
            }
        }
        throw line.refusal("an equation is d(x)/dt = …, x_next = …, x_start = …, current a -> b = …, "
                + "voltage a -> b = …, 0 = … or local name = …");
    }

    private static void requireEquals(Reading.Line line, int at) {
        if (!line.value(at).equals("=") || at + 1 >= line.size()) {
            throw line.refusal(Math.min(at, line.size() - 1), "'=' and a right side are expected here");
        }
    }

    // ─── pins and internal nodes ────────────────────────────────────────────────────────────────────

    Block.Pins pins() {
        return new Block.Pins(reading.blockLines("pins").stream().map(this::pin).toList());
    }

    /** {@code out analog output 1 7 "output"}, {@code clk digital input 3 11 edge}. */
    private Pin pin(Reading.Line line) {
        String name = line.value(0);
        List<Integer> numbers = new ArrayList<>();
        List<String> attributes = new ArrayList<>();
        PinNature nature = null;
        boolean output = false;
        String description = "";
        for (int index = 1; index < line.size(); index++) {
            String word = line.value(index);
            if (line.isString(index)) {
                description = reading.literal(line.get(index));
            } else if (word.matches("\\d+")) {
                numbers.add(Integer.parseInt(word));
            } else {
                switch (word) {
                    case "analog" -> nature = PinNature.ANALOG;
                    case "digital" -> nature = PinNature.DIGITAL;
                    case "power" -> nature = PinNature.POWER;
                    case "ground" -> nature = PinNature.GROUND;
                    case "output" -> output = true;
                    case "input" -> output = false;
                    default -> attributes.add(word);
                }
            }
        }
        if (nature == null) {
            throw line.refusal("pin '" + name + "' needs a nature: analog, digital, power or ground");
        }
        return new Pin(name, numbers, nature, output, attributes, description);
    }

    Block.Nodes nodes() {
        return new Block.Nodes(reading.blockLines("nodes").stream().map(line -> {
            boolean digital = line.value(0).equals("digital");
            int nameAt = digital ? 1 : 0;
            if (line.size() <= nameAt || line.size() > nameAt + 2
                    || (line.size() == nameAt + 2 && !line.isString(nameAt + 1))) {
                throw line.refusal("an internal node is '[digital] name [\"description\"]'");
            }
            String description = line.size() == nameAt + 2 ? reading.literal(line.get(nameAt + 1)) : "";
            return new InternalNode(line.value(nameAt), digital, description);
        }).toList());
    }
}
