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
import org.jmouse.parallax.script.Block.PinDirection;
import org.jmouse.parallax.script.Block.PinNature;
import org.jmouse.parallax.script.Block.Range;
import org.jmouse.parallax.script.Block.StateVariable;
import org.jmouse.el.type.ValueType;
import org.jmouse.parallax.script.Dimension;
import org.jmouse.parallax.script.ElectricalTypes;
import org.jmouse.parallax.script.QuantityType;
import org.jmouse.parallax.script.SourceText;

import java.util.ArrayList;
import java.util.Collections;
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
    /** The quantities the document being read named with {@code type}, by name; forgotten at the next document. */
    private final Map<String, ValueType> types = new LinkedHashMap<>();

    DeclarationReader(Reading reading) {
        this.reading = reading;
    }

    /** A new document begins: the types the last one named are not its. */
    void forgetTypes() {
        types.clear();
    }

    /**
     * {@code type power_density = power / area}: a quantity the document names, built from the known ones — jMX's
     * electrical quantities, the core's, and the document's own named earlier — by {@code *}, {@code /} and whole
     * powers ({@code current ^ 2}, {@code 1 / time}). What it says is a dimension; a {@code let} may then be typed by it.
     */
    void type(Reading.Line line) {
        if (line.size() < 3 || !line.value(1).equals("=")) {
            throw line.refusal("a type is 'type name = power / area': a name, '=', and known types joined by * and /");
        }
        String name = line.value(0);
        if (resolveType(name).isPresent()) {
            throw line.refusal(0, "'" + name + "' is already a type");
        }
        types.put(name, new QuantityType(name, "", typeExpression(line, 2)));
    }

    /** What a written type name is: the document's own first, then jMX's and the core's. */
    private Optional<ValueType> resolveType(String name) {
        return Optional.ofNullable(types.get(name)).or(() -> ElectricalTypes.find(name));
    }

    /** {@code power / area}, {@code voltage * time}, {@code current ^ 2}, {@code 1 / time}: the product the words make. */
    private Dimension typeExpression(Reading.Line line, int from) {
        Dimension result = Dimension.NONE;
        boolean dividing = false;
        int at = from;
        while (at < line.size()) {
            String word = line.value(at);
            Dimension factor;
            if (word.equals("1") && at == from) {
                factor = Dimension.NONE;
            } else {
                ValueType named = resolveType(word).orElseThrow(() -> line.refusal(from, "'" + word
                        + "' is not a type here. Known types: " + String.join(", ", knownTypeNames())));
                if (!(named instanceof QuantityType quantity)) {
                    throw line.refusal(from, "'" + word + "' is not a quantity, so it measures nothing a type can be built of");
                }
                factor = quantity.dimension();
            }
            at++;
            if (at < line.size() && line.value(at).equals("^")) {
                int exponent;
                try {
                    exponent = Integer.parseInt(line.value(at + 1));
                } catch (RuntimeException notWhole) {
                    throw line.refusal(Math.min(at + 1, line.size() - 1), "a type's power is a whole number: current ^ 2");
                }
                factor = factor.power(exponent);
                at += 2;
            }
            result = dividing ? result.over(factor) : result.times(factor);
            if (at >= line.size()) {
                break;
            }
            String operator = line.value(at);
            if (!operator.equals("*") && !operator.equals("/")) {
                throw line.refusal(at, "types are joined by * and /, not '" + operator + "'");
            }
            dividing = operator.equals("/");
            at++;
        }
        return result;
    }

    private List<String> knownTypeNames() {
        List<String> names = new ArrayList<>(types.keySet());
        names.addAll(ElectricalTypes.names());
        return names.stream().sorted().toList();
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
        if (line.isString(2)) {
            return textParameter(line, name);
        }
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

    /**
     * {@code first_line = "Hello, Parallax!" "what the first line says"}: a text, as written in its quotes, then an optional
     * description. A text is not a number: no unit, no range, no step.
     */
    /**
     * {@code name = "text" [in ["one", "other"]] ["description"]}: a text has no unit, range or step, but may be one of a
     * list — what a part line may write over it, and what an editor offers.
     */
    private Parameter textParameter(Reading.Line line, String name) {
        String shape = "a text parameter is 'name = \"text\" [in [\"one\", \"other\"]] [\"description\"]'";
        List<SourceText> choices = List.of();
        int index = 3;
        if (index < line.size() && line.value(index).equals("in")) {
            if (!line.value(index + 1).equals("[")) {
                throw line.refusal(index, shape + ": its choices are a list of texts in brackets");
            }
            int close = index + 2;
            while (close < line.size() && !line.value(close).equals("]")) {
                close++;
            }
            if (close == line.size()) {
                throw line.refusal(index + 1, shape + ": the list of choices is not closed");
            }
            choices = new ArrayList<>();
            int start = index + 2;
            for (int comma : line.commas(index + 2, close)) {
                choices.add(textChoice(line, start, comma, shape));
                start = comma + 1;
            }
            choices.add(textChoice(line, start, close, shape));
            if (choices.size() < 2) {
                throw line.refusal(index, shape + ": a list of choices names at least two");
            }
            String written = line.text(2, 3).text();
            if (choices.stream().noneMatch(choice -> choice.text().equals(written))) {
                throw line.refusal(2, written + " is not one of its own choices");
            }
            index = close + 1;
        }
        String description = "";
        if (index < line.size() && line.isString(index)) {
            description = reading.literal(line.get(index));
            index++;
        }
        if (index < line.size()) {
            throw line.refusal(index, shape + ": a text has no unit, range or step");
        }
        return new Parameter(name, line.text(2, 3), Optional.empty(), Optional.empty(), Optional.empty(),
                EnumSet.of(ParameterFlag.TEXT), description, choices);
    }

    /** One choice of a text parameter: a single text between two commas, or a comma and the closing bracket. */
    private static SourceText textChoice(Reading.Line line, int start, int end, String shape) {
        if (end - start != 1 || !line.isString(start)) {
            throw line.refusal(Math.min(start, line.size() - 1), shape + ": each choice is one text in quotes");
        }
        return line.text(start, end);
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

    /** {@code x = 1 continuous "…"}, {@code stored = X discrete}, {@code memory[256] = 0 discrete}. */
    private StateVariable stateVariable(Reading.Line line) {
        // A row of states writes its length after its name: the rest of the line reads on as for one state.
        int length = 0;
        int equals = 1;
        if (line.size() > 4 && line.value(1).equals("[")) {
            if (!line.value(2).matches("\\d+") || !line.value(3).equals("]")) {
                throw line.refusal("a row of states is 'name[length] = initial discrete', as 'memory[256] = 0 discrete'");
            }
            length = Integer.parseInt(line.value(2));
            if (length < 1 || length > 65536) {
                throw line.refusal("a row of states has 1 to 65536 slots, not " + length);
            }
            equals = 4;
        }
        int nature = line.indexOf(equals + 1, "continuous", "discrete");
        if (line.size() < equals + 3 || !line.value(equals).equals("=") || nature < 0) {
            throw line.refusal("a state is 'name = initial continuous|discrete [\"description\"]'");
        }
        if (length > 0 && !line.value(nature).equals("discrete")) {
            throw line.refusal(nature, "a row of states is discrete: the equations read numbers, not rows");
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
        return new StateVariable(line.value(0), line.text(equals + 1, nature), line.value(nature).equals("discrete"), range, description, length);
    }

    // ─── equations ──────────────────────────────────────────────────────────────────────────────────

    /**
     * {@code display { level = terminal_one - terminal_two;  charge = 100 * (terminal_one - terminal_two) / voltage_rating unit % }}:
     * a name, then the expression it shows — and, with {@code unit}, a figure written beside the part as well as drawn,
     * with an optional caption in quotes saying what it is of: {@code charge = … unit % "of the rated voltage"}. A figure
     * without a unit may have a caption too ({@code taken = acknowledged "1 when the target took the last byte"}): the
     * quoted text is never part of the expression.
     */
    Block.Display display() {
        Map<String, SourceText> figures = new LinkedHashMap<>();
        Map<String, String> units = new LinkedHashMap<>();
        Map<String, String> captions = new LinkedHashMap<>();
        for (Reading.Line line : reading.blockLines("display")) {
            if (line.size() > 1 && line.value(1).equals("[")) {
                displayRow(line, figures, captions);
                continue;
            }
            int unit = line.indexOf(2, "unit");
            boolean captioned = line.size() > 3 && line.isString(line.size() - 1);
            int end = captioned ? line.size() - 1 : line.size();
            if (line.size() < 3 || !line.value(1).equals("=") || (unit >= 0 && unit != end - 2)) {
                throw line.refusal("a display line is 'name = expression [unit U] [\"caption\"]': "
                        + "'charge = 100 * (terminal_one - terminal_two) / voltage_rating unit % \"of the rated voltage\"'");
            }
            if (figures.containsKey(line.value(0))) {
                throw line.refusal("'" + line.value(0) + "' is displayed twice");
            }
            figures.put(line.value(0), line.text(2, unit >= 0 ? unit : end));
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
     * {@code cell[32] = shown ["caption"]}: one figure for each slot of a row state — {@code cell_0 = shown_0} …
     * {@code cell_31 = shown_31} —, what a display of many cells shows: a symbol's inputs named as its figures. The
     * caption, when written, is each figure's.
     */
    private void displayRow(Reading.Line line, Map<String, SourceText> figures, Map<String, String> captions) {
        boolean captioned = line.size() == 7 && line.isString(6);
        if ((line.size() != 6 && !captioned) || !line.value(3).equals("]") || !line.value(4).equals("=")
                || !line.value(2).matches("\\d+")) {
            throw line.refusal("a row of figures is 'name[count] = row [\"caption\"]': 'cell[32] = shown', one figure for each slot");
        }
        String caption = captioned ? line.words(6, 7).getFirst() : null;
        String name = line.value(0);
        String row = line.value(5);
        int count = Integer.parseInt(line.value(2));
        SourceText written = line.text(5, 6);
        for (int index = 0; index < count; index++) {
            String figure = name + "_" + index;
            if (figures.containsKey(figure)) {
                throw line.refusal("'" + figure + "' is displayed twice");
            }
            figures.put(figure, new SourceText(row + "_" + index, written.line(), written.column(), written.offset()));
            if (caption != null) {
                captions.put(figure, caption);
            }
        }
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
     * {@code heat { d(heating)/dt = …; let name = …; temperature = … }}: the law of a part's own temperature — rates and
     * lets as in {@code equations}, and the one line that says what the temperature is.
     */
    Block.Heat heat() {
        List<Equation> lines = new ArrayList<>();
        SourceText temperature = null;
        Reading.Line first = null;
        for (Reading.Line line : reading.blockLines("heat")) {
            first = first == null ? line : first;
            if (line.value(0).equals("temperature") && line.value(1).equals("=")) {
                if (temperature != null) {
                    throw line.refusal("a heat law says what the temperature is once");
                }
                requireEquals(line, 1);
                temperature = line.text(2);
                continue;
            }
            Equation equation = equation(line);
            if (equation.form() != EquationForm.DERIVATIVE && equation.form() != EquationForm.LOCAL) {
                throw line.refusal("a heat law is d(x)/dt = …, let name = … and temperature = …");
            }
            lines.add(equation);
        }
        if (temperature == null) {
            throw first == null ? new IllegalArgumentException("a heat law needs 'temperature = …'")
                    : first.refusal("a heat law needs 'temperature = …': the part's own temperature, kelvin");
        }
        return new Block.Heat(lines, temperature);
    }

    /**
     * {@code d(x)/dt = …}, {@code x_next = …}, {@code x_start = …}, {@code current a -> b = …} (a current from pin a to
     * pin b through the part), {@code voltage a -> b = …} (a branch holding pin a at that voltage above pin b),
     * {@code 0 = …}, {@code let name = …} or {@code let name : current = …} ({@code local} is the same word).
     */
    Equation equation(Reading.Line line) {
        String head = line.value(0);
        if (head.equals("let") || head.equals("local")) {
            if (!line.value(2).equals(":")) {
                requireEquals(line, 2);
                return new Equation(EquationForm.LOCAL, line.value(1), null, null, line.text(3));
            }
            String typeName = line.value(3);
            ValueType type = resolveType(typeName).orElseThrow(() -> line.refusal(3, "'" + typeName
                    + "' is not a type here. Known types: " + String.join(", ", knownTypeNames())));
            requireEquals(line, 4);
            return new Equation(EquationForm.LOCAL, line.value(1), null, null, line.text(5), type);
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
                + "voltage a -> b = …, 0 = … or let name [: type] = …");
    }

    private static void requireEquals(Reading.Line line, int at) {
        if (!line.value(at).equals("=") || at + 1 >= line.size()) {
            throw line.refusal(Math.min(at, line.size() - 1), "'=' and a right side are expected here");
        }
    }

    // ─── pins and internal nodes ────────────────────────────────────────────────────────────────────

    Block.Pins pins() {
        List<Pin> pins = new ArrayList<>();
        List<Block.Bus> buses = new ArrayList<>();
        for (Reading.Line line : reading.blockLines("pins")) {
            if (line.size() > 1 && line.value(1).equals("[")) {
                buses.add(bus(line, pins));
            } else {
                pins.add(pin(line, line.value(0), 1));
            }
        }
        return new Block.Pins(pins, buses);
    }

    /**
     * {@code q[0..7] digital output 15, 1, 2, 3, 4, 5, 6, 7 "parallel outputs"}: a pin for each bit — {@code q_0} …
     * {@code q_7} —, each with its package number in the order the range is written, all of one nature. Bit k is always
     * {@code q_k}, the least significant first, whichever way the range runs. A bus takes as many numbers as it has bits,
     * or none; the bits of one bus are never split across units.
     */
    private Block.Bus bus(Reading.Line line, List<Pin> pins) {
        String name = line.value(0);
        int range = line.indexOf(2, "..");
        if (range != 3 || line.size() < 6 || !line.value(5).equals("]") || !line.value(2).matches("\\d+") || !line.value(4).matches("\\d+")) {
            throw line.refusal("a bus is written 'name[first..last] nature …', as 'q[0..7] digital output 15, 1, 2, …'");
        }
        int first = Integer.parseInt(line.value(2));
        int last = Integer.parseInt(line.value(4));
        int width = Math.abs(last - first) + 1;
        if (width < 2 || width > 32) {
            throw line.refusal("a bus has 2 to 32 bits, not " + width);
        }
        Pin template = pin(line, name, 6);
        if (template.nature() != PinNature.DIGITAL) {
            // A bus is read and driven as one number of levels; a voltage has no bits to make one of.
            throw line.refusal("bus '" + name + "' is " + template.nature().name().toLowerCase() + ": a bus is digital — "
                    + "write analog pins one by one, as 'channel_0 analog 1'");
        }
        if (!template.numbers().isEmpty() && template.numbers().size() != width) {
            throw line.refusal("bus '" + name + "' has " + width + " bits and " + template.numbers().size()
                    + " package numbers: write one number for each bit, in the order of its bits, or none");
        }
        int step = last >= first ? 1 : -1;
        List<String> bits = new ArrayList<>();
        for (int position = 0; position < width; position++) {
            String bit = name + "_" + (first + position * step);
            List<Integer> number = template.numbers().isEmpty() ? List.of() : List.of(template.numbers().get(position));
            pins.add(new Pin(bit, number, template.nature(), template.direction(), template.attributes(), template.description()));
            bits.add(bit);
        }
        if (step < 0) {
            // q_k is bit k however the range is written: q[7..0] lists its package numbers from q_7 down, and q_0 is still
            // the least significant bit, as a data sheet and every HDL read it.
            Collections.reverse(bits);
        }
        return new Block.Bus(name, bits);
    }

    /** {@code out analog output 1 7 "output"}, {@code clk digital input 3 11 edge}: its words from {@code from} on. */
    private Pin pin(Reading.Line line, String name, int from) {
        List<Integer> numbers = new ArrayList<>();
        List<String> attributes = new ArrayList<>();
        PinNature nature = null;
        PinDirection direction = PinDirection.INPUT;
        String description = "";
        for (int index = from; index < line.size(); index++) {
            String word = line.value(index);
            if (line.isString(index)) {
                description = reading.literal(line.get(index));
            } else if (word.matches("\\d+")) {
                numbers.add(Integer.parseInt(word));
            } else if (!word.equals(",")) {
                // A comma only parts numbers — '15, 1, 2' is three of them —; every other word says something, and
                // may be hyphenated: 'open-drain' is one word.
                word = line.joinedWord(index);
                index = line.joinedEnd(index) - 1;
                switch (word) {
                    case "analog" -> nature = PinNature.ANALOG;
                    case "digital" -> nature = PinNature.DIGITAL;
                    case "power" -> nature = PinNature.POWER;
                    case "ground" -> nature = PinNature.GROUND;
                    case "output" -> direction = PinDirection.OUTPUT;
                    case "input" -> direction = PinDirection.INPUT;
                    case "bidirectional" -> direction = PinDirection.BIDIRECTIONAL;
                    default -> attributes.add(word);
                }
            }
        }
        if (nature == null) {
            throw line.refusal("pin '" + name + "' needs a nature: analog, digital, power or ground");
        }
        return new Pin(name, numbers, nature, direction, attributes, description);
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
