package org.jmouse.parallax.script.reader;

import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.type.ValueType;
import org.jmouse.parallax.script.Block;
import org.jmouse.parallax.script.Block.Equation;
import org.jmouse.parallax.script.Block.EquationForm;
import org.jmouse.parallax.script.Block.Pin;
import org.jmouse.parallax.script.ElectricalTypes;
import org.jmouse.parallax.script.ExperienceParseException;
import org.jmouse.parallax.script.QuantityType;
import org.jmouse.parallax.script.SourceText;
import org.jmouse.units.Dimension;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The declaration blocks every computable kind shares: parameters, inputs, outputs, state, display, measure, leads,
 * image, equations, heat, pins, nodes — and the {@code type} statement. Each block's lines are read by its
 * {@link JmxLineParser}; a block gathers what they declare and holds them to the rules that span lines.
 */
final class DeclarationReader {

    private final Reading reading;
    /** The quantities the document being read named with {@code type}, by name; forgotten at the next document. */
    private final Map<String, ValueType> types = new LinkedHashMap<>();
    private final ParameterLineParser parameterLines;
    private final InputLineParser inputLines;
    private final OutputLineParser outputLines;
    private final StateLineParser stateLines;
    private final DisplayLineParser displayLines;
    private final MeasureLineParser measureLines;
    private final LeadLineParser leadLines;
    private final ImageLineParser imageLines;
    private final EquationLineParser equationLines;
    private final PinLineParser pinLines;
    private final NodeLineParser nodeLines;

    DeclarationReader(Reading reading) {
        this.reading = reading;
        JmxValues values = new JmxValues(reading);
        this.parameterLines = new ParameterLineParser(reading, values);
        this.inputLines = new InputLineParser(reading, values);
        this.outputLines = new OutputLineParser(reading, values);
        this.stateLines = new StateLineParser(reading, values);
        this.displayLines = new DisplayLineParser(reading, values);
        this.measureLines = new MeasureLineParser(reading, values);
        this.leadLines = new LeadLineParser(reading);
        this.imageLines = new ImageLineParser(reading, values);
        this.equationLines = new EquationLineParser(reading, values, this);
        this.pinLines = new PinLineParser(reading);
        this.nodeLines = new NodeLineParser(reading);
    }

    /** A new document begins: the types the last one named are not its. */
    void forgetTypes() {
        types.clear();
    }

    // ─── types ──────────────────────────────────────────────────────────────────────────────────────

    /**
     * {@code type power_density = power / area}: a quantity the document names, built from the known ones — jMX's
     * electrical quantities, the core's, and the document's own named earlier — by {@code *}, {@code /} and whole
     * powers ({@code current ^ 2}, {@code 1 / time}). What it says is a dimension; a {@code let} may then be typed by it.
     */
    void type() {
        TokenCursor line = reading.lineView();
        Token first = reading.cursor.current();
        if (line.remaining() < 3 || !line.checkValueAt(1, "=")) {
            throw reading.refusal(first, "a type is 'type name = power / area': a name, '=', and known types joined by * and /");
        }
        String name = line.valueAt(0);
        if (resolveType(name).isPresent()) {
            throw reading.refusalAhead(0, "'" + name + "' is already a type");
        }
        types.put(name, new QuantityType(name, "", typeExpression(line, 2)));
        reading.skipLine();
    }

    /** What a written type name is: the document's own first, then jMX's and the core's. */
    Optional<ValueType> resolveType(String name) {
        return Optional.ofNullable(types.get(name)).or(() -> ElectricalTypes.find(name));
    }

    /** Every type a document may name, its own and the known ones, in order. */
    List<String> knownTypeNames() {
        List<String> names = new ArrayList<>(types.keySet());
        names.addAll(ElectricalTypes.names());
        return names.stream().sorted().toList();
    }

    /** {@code power / area}, {@code voltage * time}, {@code current ^ 2}, {@code 1 / time}: the product the words make. */
    private Dimension typeExpression(TokenCursor line, int from) {
        Dimension result = Dimension.NONE;
        boolean dividing = false;
        int at = from;
        int length = line.remaining();
        while (at < length) {
            String word = line.valueAt(at);
            Dimension factor;
            if (word.equals("1") && at == from) {
                factor = Dimension.NONE;
            } else {
                ValueType named = resolveType(word).orElseThrow(() -> reading.refusalAhead(from, "'" + word
                        + "' is not a type here. Known types: " + String.join(", ", knownTypeNames())));
                if (!(named instanceof QuantityType quantity)) {
                    throw reading.refusalAhead(from, "'" + word + "' is not a quantity, so it measures nothing a type can be built of");
                }
                factor = quantity.dimension();
            }
            at++;
            if (at < length && line.checkValueAt(at, "^")) {
                int exponent;
                try {
                    exponent = Integer.parseInt(line.valueAt(at + 1));
                } catch (RuntimeException notWhole) {
                    throw reading.refusalAhead(Math.min(at + 1, length - 1), "a type's power is a whole number: current ^ 2");
                }
                factor = factor.power(exponent);
                at += 2;
            }
            result = dividing ? result.over(factor) : result.times(factor);
            if (at >= length) {
                break;
            }
            String operator = line.valueAt(at);
            if (!operator.equals("*") && !operator.equals("/")) {
                throw reading.refusalAhead(at, "types are joined by * and /, not '" + operator + "'");
            }
            dividing = operator.equals("/");
            at++;
        }
        return result;
    }

    // ─── blocks of lines ────────────────────────────────────────────────────────────────────────────

    Block.Parameters parameters() {
        return new Block.Parameters(reading.lines("parameters", parameterLines));
    }

    Block.Inputs inputs() {
        return new Block.Inputs(reading.lines("inputs", inputLines));
    }

    Block.Outputs outputs() {
        return new Block.Outputs(reading.lines("outputs", outputLines));
    }

    Block.State state() {
        return new Block.State(reading.lines("state", stateLines));
    }

    Block.Equations equations() {
        return new Block.Equations(reading.lines("equations", equationLines));
    }

    Block.Nodes nodes() {
        return new Block.Nodes(reading.lines("nodes", nodeLines));
    }

    /**
     * {@code display { level = terminal_one - terminal_two;  charge = 100 * (terminal_one - terminal_two) / voltage_rating unit % }}:
     * a name, then the expression it shows — and, with {@code unit}, a figure written beside the part as well as drawn,
     * with an optional caption in quotes saying what it is of. A row, {@code cell[32] = shown}, is a figure for each slot.
     */
    Block.Display display() {
        Map<String, SourceText> figures = new LinkedHashMap<>();
        Map<String, String> units = new LinkedHashMap<>();
        Map<String, String> captions = new LinkedHashMap<>();
        for (DisplayLineParser.Figure figure : reading.lines("display", displayLines)) {
            if (figure.count() == 0) {
                put(figures, figure.name(), figure.expression(), figure.first());
                figure.unit().ifPresent(unit -> units.put(figure.name(), unit));
                figure.caption().ifPresent(caption -> captions.put(figure.name(), caption));
                continue;
            }
            SourceText written = figure.expression();
            for (int index = 0; index < figure.count(); index++) {
                String name = figure.name() + "_" + index;
                put(figures, name, new SourceText(written.text() + "_" + index, written.line(), written.column(),
                        written.offset()), figure.first());
                figure.caption().ifPresent(caption -> captions.put(name, caption));
            }
        }
        return new Block.Display(figures, units, captions);
    }

    private void put(Map<String, SourceText> figures, String name, SourceText expression, Token first) {
        if (figures.containsKey(name)) {
            throw reading.refusal(first, "'" + name + "' is displayed twice");
        }
        figures.put(name, expression);
    }

    /**
     * {@code measure { reading = plus - minus unit V signal }}: a name, then the expression it reads, then {@code unit} and
     * what that comes out in, and {@code signal} when it is a signal a bench reads as DC, AC or RMS.
     */
    Block.Measure measure() {
        Token open = reading.cursor.current();
        Map<String, Block.MeasureFigure> figures = new LinkedHashMap<>();
        for (MeasureLineParser.Measured measured : reading.lines("measure", measureLines)) {
            if (figures.containsKey(measured.name())) {
                throw reading.refusal(measured.first(), "'" + measured.name() + "' is measured twice");
            }
            figures.put(measured.name(), measured.figure());
        }
        if (figures.isEmpty()) {
            throw reading.refusal(open, "a measure block names what the instrument reads: 'reading = plus - minus unit V'");
        }
        return new Block.Measure(figures);
    }

    /** {@code leads { plus red stripes black }}: a pin, its lead's colour, and optionally {@code stripes} and theirs. */
    Block.Leads leads() {
        Map<String, Block.LeadLook> looks = new LinkedHashMap<>();
        for (LeadLineParser.Lead lead : reading.lines("leads", leadLines)) {
            if (looks.containsKey(lead.pin())) {
                throw reading.refusal(lead.first(), "'" + lead.pin() + "' has its lead described twice");
            }
            looks.put(lead.pin(), lead.look());
        }
        return new Block.Leads(looks);
    }

    /**
     * {@code image { … }}, one statement a line: {@code over X in a .. b, Y in c .. d}, {@code start u = …} for every state
     * variable, {@code stop when …} (any number), {@code hit when … crosses 0 [where …]}, {@code value = …}.
     */
    Block.Image image() {
        Token open = reading.cursor.current();
        ImageLineParser.Over over = null;
        Map<String, SourceText> starts = new LinkedHashMap<>();
        List<SourceText> stops = new ArrayList<>();
        ImageLineParser.Hit hit = null;
        SourceText value = null;
        for (ImageLineParser.Statement statement : reading.lines("image", imageLines)) {
            switch (statement) {
                case ImageLineParser.Over written -> over = written;
                case ImageLineParser.Start start -> starts.put(start.variable(), start.value());
                case ImageLineParser.Stop stop -> stops.add(stop.condition());
                case ImageLineParser.Hit written -> hit = written;
                case ImageLineParser.Value written -> value = written.value();
            }
        }
        if (over == null || hit == null || value == null || starts.isEmpty() || stops.isEmpty()) {
            throw reading.refusal(open, "an image needs 'over', a 'start' for every state variable, at least one "
                    + "'stop when', a 'hit when' and a 'value'");
        }
        return new Block.Image(over.horizontal(), over.horizontalRange(), over.vertical(), over.verticalRange(), starts,
                stops, hit.crossing(), hit.where(), value);
    }

    /**
     * {@code heat { d(heating)/dt = …; let name = …; temperature = … }}: the law of a part's own temperature — rates and
     * lets as in {@code equations}, and the one line that says what the temperature is.
     */
    Block.Heat heat() {
        List<Equation> lines = new ArrayList<>();
        SourceText temperature = null;
        Token first = null;
        List<TokenCursor.Savepoint> starts = reading.lineStarts("heat");
        TokenCursor.Savepoint after = reading.cursor.savepoint();
        for (TokenCursor.Savepoint start : starts) {
            reading.cursor.restore(start);
            Token line = reading.cursor.current();
            first = first == null ? line : first;
            if (equationLines.atTemperature()) {
                if (temperature != null) {
                    throw reading.refusal(line, "a heat law says what the temperature is once");
                }
                temperature = equationLines.temperature();
                continue;
            }
            Equation equation = equationLines.read();
            if (equation.form() != EquationForm.DERIVATIVE && equation.form() != EquationForm.LOCAL) {
                throw reading.refusal(line, "a heat law is d(x)/dt = …, let name = … and temperature = …");
            }
            lines.add(equation);
        }
        reading.cursor.restore(after);
        if (temperature == null) {
            throw first == null ? new IllegalArgumentException("a heat law needs 'temperature = …'")
                    : reading.refusal(first, "a heat law needs 'temperature = …': the part's own temperature, kelvin");
        }
        return new Block.Heat(lines, temperature);
    }

    /**
     * {@code pins { out analog output 1 "output"; q[0..7] digital output 15, 1, 2, … }}: every pin, and every bus with
     * the pins of its bits.
     */
    Block.Pins pins() {
        List<Pin> pins = new ArrayList<>();
        List<Block.Bus> buses = new ArrayList<>();
        for (PinLineParser.Declared declared : reading.lines("pins", pinLines)) {
            pins.addAll(declared.pins());
            declared.bus().ifPresent(buses::add);
        }
        return new Block.Pins(pins, buses);
    }
}
