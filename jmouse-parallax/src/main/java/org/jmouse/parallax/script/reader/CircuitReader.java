package org.jmouse.parallax.script.reader;

import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.parallax.script.Block;
import org.jmouse.parallax.script.Block.AnalysisKind;
import org.jmouse.parallax.script.Block.Criterion;
import org.jmouse.parallax.script.Block.Part;
import org.jmouse.parallax.script.Block.Probe;
import org.jmouse.parallax.script.SourceText;
import org.jmouse.units.Quantity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Circuits, and everything else that is a netlist: a circuit's parts, a component's internal parts and an
 * expectation's bench are written in one notation — {@code ID reference (connections) [settings] [symbol S]} —; and
 * the blocks around them: layout, nets, ports, probes, analysis, sheet, expectations. Each block's lines are read by its
 * {@link JmxLineParser}; a block gathers what they declare.
 */
final class CircuitReader {

    /** The words an analysis line writes its settings with. */
    private static final List<String> ANALYSIS_SETTINGS = List.of("for", "from", "to", "points", "source", "step");

    /** A sheet's grid when it names none. */
    private static final double DEFAULT_GRID = 20;

    private final Reading reading;
    private final JmxValues values;
    private final PartLineParser partLines;
    private final LayoutLineParser layoutLines;
    private final NetLineParser netLines;
    private final PortLineParser portLines;
    private final ProbeLineParser probeLines;
    private final CriterionLineParser criterionLines;

    CircuitReader(Reading reading) {
        this.reading = reading;
        this.values = new JmxValues(reading);
        this.partLines = new PartLineParser(reading, values);
        this.layoutLines = new LayoutLineParser(reading, values);
        this.netLines = new NetLineParser(reading, values);
        this.portLines = new PortLineParser(reading);
        this.probeLines = new ProbeLineParser(reading, values);
        this.criterionLines = new CriterionLineParser(reading, values);
    }

    Block.Parts parts() {
        return new Block.Parts(partLines("parts"));
    }

    List<Part> partLines(String block) {
        return reading.lines(block, partLines);
    }

    /**
     * {@code layout { R1 at 240, 200 rotate 90;  GND1 ground at 600, 520;  A1 ground agnd at 640, 520;  W1 wire R1.negative
     * to C1.positive via … }}.
     */
    Block.Layout layout() {
        layoutLines.begin();
        List<Block.LayoutItem> items = new ArrayList<>();
        List<Block.LayoutWire> wires = new ArrayList<>();
        for (LayoutLineParser.Laid laid : reading.lines("layout", layoutLines)) {
            switch (laid) {
                case LayoutLineParser.Item item -> items.add(item.item());
                case LayoutLineParser.Wire wire -> wires.add(wire.wire());
            }
        }
        return new Block.Layout(items, wires);
    }

    Block.Nets nets() {
        return new Block.Nets(reading.lines("nets", netLines));
    }

    Block.Ports ports() {
        return new Block.Ports(reading.lines("ports", portLines));
    }

    /** The channels, and at most one {@code time 5 ms/div}: the time base the oscilloscope opens on. */
    Block.Probes probes() {
        List<Probe> probes = new ArrayList<>();
        Optional<Quantity> timePerDivision = Optional.empty();
        for (ProbeLineParser.Probed probed : reading.lines("probes", probeLines)) {
            switch (probed) {
                case ProbeLineParser.Channel channel -> probes.add(channel.probe());
                case ProbeLineParser.TimeBase timeBase -> {
                    if (timePerDivision.isPresent()) {
                        throw reading.refusal(timeBase.first(), "the oscilloscope has one time base; this block names it twice");
                    }
                    timePerDivision = Optional.of(timeBase.perDivision());
                }
            }
        }
        return new Block.Probes(probes, timePerDivision);
    }

    // ─── single-line blocks ─────────────────────────────────────────────────────────────────────────

    /** {@code analysis transient for 5 ms}, {@code analysis ac from 10 Hz to 1 MHz points 200}. */
    Block.Analysis analysis(Token keyword) {
        TokenCursor cursor = reading.cursor;
        if (reading.atLineEnd()) {
            throw reading.refusal(keyword, "an analysis is 'analysis transient|ac|operating|dc_sweep [settings]'");
        }
        Token kindWord = cursor.next();
        AnalysisKind kind;
        try {
            kind = AnalysisKind.valueOf(kindWord.value().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException unknown) {
            throw reading.refusal(kindWord, "'" + kindWord.value() + "' is not an analysis: transient, ac, operating or dc_sweep");
        }
        Map<String, SourceText> settings = new LinkedHashMap<>();
        while (!reading.atLineEnd()) {
            Token key = cursor.next();
            if (!ANALYSIS_SETTINGS.contains(key.value())) {
                throw reading.refusal(key, "'" + key.value() + "' is not an analysis setting: for, from, to, points, "
                        + "source or step");
            }
            settings.put(key.value(), values.expression(Set.copyOf(ANALYSIS_SETTINGS)));
        }
        return new Block.Analysis(kind, settings);
    }

    /** {@code sheet 1200 x 760 grid 20}. */
    Block.Sheet sheet(Token keyword) {
        TokenCursor cursor = reading.cursor;
        if (reading.lineView().offsetOf(0, "x") < 0) {
            throw reading.refusal(keyword, "a sheet is 'sheet width x height [grid step]'");
        }
        double width = values.quantity(JmxValues.words(Set.of("x")));
        cursor.next();
        double height = values.quantity(JmxValues.words(Set.of("grid")));
        double grid = DEFAULT_GRID;
        if (!reading.atLineEnd()) {
            cursor.next();
            grid = values.quantity(JmxValues.LINE);
        }
        return new Block.Sheet(width, height, grid);
    }

    // ─── expectations ───────────────────────────────────────────────────────────────────────────────

    /**
     * A circuit's expectation measures the circuit itself; a component's needs a bench to put the part in.
     *
     * <pre>
     * expect "astable frequency" from "datasheet" {
     *     bench for 3 ms { … parts … }
     *     measure frequency V(out) above 2.5 V after 1 ms
     *     near 6.86 kHz +- 5%
     * }
     * </pre>
     */
    Block.Expectation expectation(Token keyword) {
        Token title = reading.expect(BasicToken.T_STRING, "the expectation's title in quotes");
        String reference = null;
        if (reading.atWord("from")) {
            reading.cursor.next();
            reference = reading.literal(reading.expect(BasicToken.T_STRING, "where the expectation comes from, in quotes"));
        }
        ExpectationBody body = new ExpectationBody();
        reading.body("'{' to open the expectation",
                () -> reading.refusal(keyword, "expectation '" + reading.literal(title) + "' is never closed"),
                body::read);
        if (body.measure.isEmpty() || body.criterion == null) {
            throw reading.refusal(keyword, "expectation '" + reading.literal(title)
                    + "' needs a measure and a criterion (near, within or is)");
        }
        return new Block.Expectation(reading.literal(title), reference, body.bench, body.sharedBench, body.duration,
                body.temperature, body.realistic, body.measure, body.criterion, reading.lineOf(keyword));
    }

    /** What an expectation's braces say, gathered statement by statement: its bench, its measure, its criterion. */
    private final class ExpectationBody {
        List<Part> bench = List.of();
        String sharedBench;
        double duration;
        Double temperature;
        boolean realistic;
        List<String> measure = List.of();
        Criterion criterion;

        void read() {
            TokenCursor cursor = reading.cursor;
            if (reading.atWord("bench")) {
                cursor.next();
                benchHeader();
                bench = partLines("bench");
            } else if (reading.atWord("measure")) {
                cursor.next();
                List<String> words = new ArrayList<>();
                while (!reading.atLineEnd()) {
                    Token token = cursor.next();
                    words.add(reading.isString(token) ? reading.literal(token) : token.value());
                }
                measure = words;
            } else {
                criterion = (Criterion) ((JmxLineParser.LineNode<?>) criterionLines.parse(cursor, reading.context)).declared();
            }
        }

        /** {@code [shared] [for duration] [at temperature] [realistic]}, before the bench's braces. */
        private void benchHeader() {
            TokenCursor cursor = reading.cursor;
            Token first = cursor.current();
            if (!reading.atLineEnd() && !cursor.checkValueAt(0, "for")) {
                // A shared bench named first: 'bench i2c-bus for 1 ms { … }' adds these parts to it.
                sharedBench = reading.joinedWord();
            }
            if (reading.atLineEnd()) {
                return;
            }
            if (!cursor.checkValueAt(0, "for")) {
                throw reading.refusal(first, "a bench is 'bench [shared] [for duration] [at temperature] [realistic] { parts }'");
            }
            TokenCursor header = reading.lineView();
            // A realistic bench is the line's last word: its instruments load what they read.
            realistic = header.checkValueAt(header.remaining() - 1, "realistic");
            cursor.next();
            duration = values.quantity(JmxValues.words(Set.of("at", "realistic")));
            if (cursor.checkValueAt(0, "at") && !reading.atLineEnd()) {
                cursor.next();
                temperature = values.quantity(JmxValues.words(Set.of("realistic")));
            }
            if (realistic) {
                cursor.next();
            }
        }
    }

    /** The criterion of the line the cursor stands on: what a graph's expectation holds its measure to as well. */
    Criterion criterion() {
        return criterionLines.read();
    }

    /** {@code bench i2c-bus { parts }}: parts written once, for every expectation that names the bench. */
    Block.Bench sharedBench(Token keyword) {
        if (reading.atLineEnd()) {
            throw reading.refusal(keyword, "a shared bench is 'bench name { parts }': 'bench i2c-bus { V1 …; R1 … }'");
        }
        String name = reading.joinedWord();
        if (!reading.atLineEnd()) {
            throw reading.refusal(keyword, "a shared bench is 'bench name { parts }': 'bench i2c-bus { V1 …; R1 … }'");
        }
        return new Block.Bench(name, partLines("bench"));
    }
}
