package org.jmouse.parallax.script.reader;

import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.Token;
import org.jmouse.parallax.script.Block;
import org.jmouse.parallax.script.Block.AnalysisKind;
import org.jmouse.parallax.script.Block.Criterion;
import org.jmouse.parallax.script.Block.CriterionKind;
import org.jmouse.parallax.script.Block.Net;
import org.jmouse.parallax.script.Block.Part;
import org.jmouse.parallax.script.Block.Placement;
import org.jmouse.parallax.script.Block.Point;
import org.jmouse.parallax.script.Block.Port;
import org.jmouse.parallax.script.Block.PortDirection;
import org.jmouse.parallax.script.Block.Probe;
import org.jmouse.datamap.Quantity;
import org.jmouse.parallax.script.SourceText;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Circuits, and everything else that is a netlist: a circuit's parts, a component's internal parts and an
 * expectation's bench are written in one notation — {@code ID reference [unit U] (connections) [settings] [place …]}.
 */
final class CircuitReader {

    /** The layout marks that name the net they are: {@code ground agnd}, {@code label vp}, {@code port FB}, {@code global vcc}. */
    private static final Set<String> NAMING_MARKS = Set.of("ground", "label", "port", "global");

    private final Reading reading;

    CircuitReader(Reading reading) {
        this.reading = reading;
    }

    // ─── parts ──────────────────────────────────────────────────────────────────────────────────────

    Block.Parts parts() {
        return new Block.Parts(partLines("parts"));
    }

    List<Part> partLines(String block) {
        return reading.blockLines(block).stream().map(this::part).toList();
    }

    /**
     * {@code R1 resistor (in, out) resistance = R place 240, 200 rotate 90},
     * {@code U1 lm358@3 (A.non_inverting_input = ref, A.inverting_input = out, A.output = out, Vcc = vcc, GND = 0)} — a
     * unit's pin named with its unit —, {@code SW1 switch (a, b) closed},
     * {@code R2 resistor (a, b) resistance = R symbol resistor-iec} — drawn with another of the component's symbols.
     */
    Part part(Reading.Line line) {
        if (line.size() < 2) {
            throw line.refusal("a part is 'ID component (connections) [settings] [place x, y]'");
        }
        String id = line.value(0);
        String reference = line.joinedWord(1);
        int index = line.joinedEnd(1);
        Optional<String> unit = Optional.empty();
        if (line.value(index).equals("unit")) {
            // It was read and then dropped, which left every pin it named unconnected and the part silently dead.
            throw line.refusal(index, "name a unit's pins with the unit: '(A.output = out, A.inverting_input = out, Vcc = vcc)'");
        }
        List<String> positional = new ArrayList<>();
        Map<String, String> named = new LinkedHashMap<>();
        if (line.value(index).equals("(")) {
            int close = line.closing(index);
            int start = index + 1;
            List<Integer> commas = line.commas(start, close);
            List<int[]> spans = new ArrayList<>();
            for (int comma : commas) {
                spans.add(new int[] {start, comma});
                start = comma + 1;
            }
            spans.add(new int[] {start, close});
            for (int[] span : spans) {
                if (span[0] >= span[1]) {
                    continue;
                }
                int equals = -1;
                for (int at = span[0]; at < span[1]; at++) {
                    if (line.value(at).equals("=")) {
                        equals = at;
                        break;
                    }
                }
                if (equals < 0) {
                    positional.add(String.join("", line.words(span[0], span[1])));
                } else {
                    named.put(String.join("", line.words(span[0], equals)), String.join("", line.words(equals + 1, span[1])));
                }
            }
            if (!positional.isEmpty() && !named.isEmpty()) {
                throw line.refusal(index, "connect a part either by pin order '(a, b)' or by name '(pin = net)', not both");
            }
            index = close + 1;
        }
        int placeAt = line.indexOf(index, "place");
        if (placeAt >= 0) {
            throw line.refusal(placeAt, "a part is placed in the 'layout' block: 'layout { " + id + " at 240, 200 }'");
        }
        int symbolAt = line.indexOf(index, "symbol");
        int settingsEnd = symbolAt >= 0 ? symbolAt : line.size();
        Map<String, SourceText> settings = settings(line, index, settingsEnd);
        Optional<String> symbol = Optional.empty();
        if (symbolAt >= 0) {
            int symbolEnd = line.size();
            if (symbolAt + 1 >= symbolEnd || line.joinedEnd(symbolAt + 1) != symbolEnd) {
                throw line.refusal(symbolAt, "'symbol' names one of the component's symbols: 'symbol resistor-iec'");
            }
            symbol = Optional.of(line.joinedWord(symbolAt + 1));
        }
        return new Part(id, reference, unit, positional, named, settings, symbol, reading.lineOf(line.first()));
    }

    /**
     * {@code resistance = R, waveform = step} or bare words such as {@code closed}: each setting as written. Commas
     * between settings are optional.
     */
    private Map<String, SourceText> settings(Reading.Line line, int from, int to) {
        Map<String, SourceText> settings = new LinkedHashMap<>();
        int index = from;
        while (index < to) {
            if (line.value(index).equals(",")) {
                index++;
                continue;
            }
            String name = line.value(index);
            if (line.value(index + 1).equals("=") && index + 1 < to) {
                int end = index + 2;
                List<Integer> commas = line.commas(index + 2, to);
                int next = nextSettingStart(line, index + 2, to);
                end = commas.isEmpty() ? next : Math.min(commas.getFirst(), next);
                settings.put(name, line.text(index + 2, end));
                index = end;
            } else {
                settings.put(name, line.text(index, index));
                index++;
            }
        }
        return settings;
    }

    /** Where the next {@code name = …} begins, so {@code a = 1 b = 2} splits without commas. */
    private static int nextSettingStart(Reading.Line line, int from, int to) {
        for (int index = from + 1; index + 1 < to; index++) {
            if (line.get(index).type() == BasicToken.T_IDENTIFIER && line.value(index + 1).equals("=")
                    && !line.value(index - 1).equals("(")) {
                return index;
            }
        }
        return to;
    }

    // ─── layout ─────────────────────────────────────────────────────────────────────────────────────

    /**
     * {@code layout { R1 at 240, 200 rotate 90;  GND1 ground at 600, 520;  A1 ground agnd at 640, 520;  W1 wire R1.negative
     * to C1.positive via … }}.
     */
    Block.Layout layout() {
        List<Block.LayoutItem> items = new ArrayList<>();
        List<Block.LayoutWire> wires = new ArrayList<>();
        java.util.Set<String> ids = new java.util.HashSet<>();
        for (Reading.Line line : reading.blockLines("layout")) {
            // An id is one word, joined where nothing separates its tokens: the canvas names a wire 'wire-3'.
            String id = line.joinedWord(0);
            int next = line.joinedEnd(0);
            if (line.size() < next + 2) {
                throw line.refusal("a layout line is 'ID at x, y', 'ID ground [net] at x, y', 'ID label|port|global NET at x, y', 'ID common at x, y', 'ID junction at x, y' or 'ID wire from to to'");
            }
            if (!ids.add(id)) {
                throw line.refusal("'" + id + "' is laid out twice");
            }
            String word = line.value(next);
            if (word.equals("wire")) {
                wires.add(wire(line, id, next + 1));
            } else if (word.equals("at")) {
                items.add(new Block.LayoutItem(id, Optional.empty(), Optional.empty(), placement(line, next + 1)));
            } else if ((word.equals("ground") || word.equals("common") || word.equals("junction")) && line.value(next + 1).equals("at")) {
                items.add(new Block.LayoutItem(id, Optional.of(word), Optional.empty(), placement(line, next + 2)));
            } else if (NAMING_MARKS.contains(word) && line.size() > next + 2 && line.value(next + 2).equals("at")) {
                // Another ground: the mark is the net it names, kept apart from node 0 by the engine. A label is a net's
                // flag, as KiCad draws a power port: every label of one net is that net, with no wire drawn between them —
                // the ±15 V rails reach an op-amp's supply pins without crossing the sheet. A port draws a net the sheet
                // hands to its parent, a global one a net every sheet of the project shares.
                items.add(new Block.LayoutItem(id, Optional.of(word), Optional.of(line.value(next + 1)), placement(line, next + 3)));
            } else {
                throw line.refusal(next, "after '" + id + "' comes 'at x, y', 'ground [net] at', 'label|port|global NET at', 'common at', 'junction at' or 'wire'");
            }
        }
        return new Block.Layout(items, wires);
    }

    /** {@code W1 wire R1.negative to C1.positive via 240 120, 300 120}. */
    private Block.LayoutWire wire(Reading.Line line, String id, int from) {
        int toAt = line.indexOf(from, "to");
        if (toAt < 0 || toAt == from || line.joinedEnd(from) != toAt) {
            throw line.refusal(from, "a wire is 'ID wire PART.pin to PART.pin [via x y, x y …]'");
        }
        int viaAt = line.indexOf(toAt, "via");
        int toEnd = viaAt < 0 ? line.size() : viaAt;
        if (toAt + 1 >= toEnd || line.joinedEnd(toAt + 1) != toEnd) {
            throw line.refusal(toAt, "a wire is 'ID wire PART.pin to PART.pin [via x y, x y …]'");
        }
        List<Point> bends = new ArrayList<>();
        if (viaAt >= 0) {
            List<Integer> ends = new ArrayList<>(line.commas(viaAt + 1, line.size()));
            ends.add(line.size());
            int start = viaAt + 1;
            for (int end : ends) {
                String[] coordinates = line.text(start, end).text().strip().split("\s+(?=[-+]?[0-9.])");
                if (coordinates.length != 2) {
                    throw line.refusal(start, "a corner is 'x y': 'via 240 120, 300 120'");
                }
                bends.add(new Point(coordinate(line, start, coordinates[0]), coordinate(line, start, coordinates[1])));
                start = end + 1;
            }
        }
        return new Block.LayoutWire(id, line.joinedWord(from), line.joinedWord(toAt + 1), bends);
    }

    private static double coordinate(Reading.Line line, int at, String written) {
        return Values.number(written).orElseThrow(() -> line.refusal(at, "'" + written + "' is not a coordinate"));
    }

    /** The texts of a part that can be put somewhere by hand, in the order a layout line writes them. */
    static final List<String> LABELS = List.of("name", "value", "readings");
    /** What an instrument's display may be set to read: its mode, and whether it shows a figure or a graph. */
    static final List<String> SHOWS = List.of("dc", "ac", "rms", "figure", "gauge", "graph");

    /**
     * {@code x, y [rotate degrees] [mirror] [name at dx, dy] [value at dx, dy] [readings at dx, dy] [shows rms graph]},
     * after {@code at}: a text's place is measured from the part's centre.
     */
    private Placement placement(Reading.Line line, int from) {
        List<Integer> keywords = new ArrayList<>();
        for (int index = from; index < line.size(); index++) {
            String word = line.value(index);
            if (word.equals("rotate") || word.equals("mirror") || word.equals("shows") || LABELS.contains(word) && line.value(index + 1).equals("at")) {
                keywords.add(index);
            }
        }
        int coordinatesEnd = keywords.isEmpty() ? line.size() : keywords.getFirst();
        List<Integer> commas = line.commas(from, coordinatesEnd);
        if (commas.isEmpty()) {
            throw line.refusal(from, "a position is 'at x, y [rotate degrees] [mirror] [name at dx, dy]'");
        }
        double x = Values.quantity(line, from, commas.getFirst());
        double y = Values.quantity(line, commas.getFirst() + 1, coordinatesEnd);
        double rotation = 0;
        boolean mirrored = false;
        java.util.Map<String, Block.Point> labels = new java.util.LinkedHashMap<>();
        List<String> shows = new ArrayList<>();
        for (int position = 0; position < keywords.size(); position++) {
            int at = keywords.get(position);
            int end = position + 1 < keywords.size() ? keywords.get(position + 1) : line.size();
            String word = line.value(at);
            if (word.equals("rotate")) {
                rotation = Values.number(line.text(at + 1, end).text())
                        .orElseThrow(() -> line.refusal(at, "a rotation is a number of degrees"));
            } else if (word.equals("mirror")) {
                mirrored = true;
            } else if (word.equals("shows")) {
                for (int index = at + 1; index < end; index++) {
                    String shown = line.value(index);
                    if (!SHOWS.contains(shown) || shows.contains(shown)) {
                        throw line.refusal(index, "a display shows one of " + String.join(", ", SHOWS) + ", each once: 'shows rms graph'");
                    }
                    shows.add(shown);
                }
            } else {
                List<Integer> comma = line.commas(at + 2, end);
                if (comma.isEmpty() || labels.containsKey(word)) {
                    throw line.refusal(at, "a text stands '" + word + " at dx, dy' from the part's centre, once");
                }
                labels.put(word, new Block.Point(Values.quantity(line, at + 2, comma.getFirst()), Values.quantity(line, comma.getFirst() + 1, end)));
            }
        }
        return new Placement(x, y, rotation, mirrored, labels, shows);
    }

    // ─── nets, ports, probes ────────────────────────────────────────────────────────────────────────

    Block.Nets nets() {
        return new Block.Nets(reading.blockLines("nets").stream().map(line -> {
            boolean ground = false;
            boolean global = false;
            List<Point> route = new ArrayList<>();
            int index = 1;
            while (index < line.size()) {
                String word = line.value(index);
                if (word.equals("ground")) {
                    ground = true;
                    index++;
                } else if (word.equals("global")) {
                    global = true;
                    index++;
                } else if (word.equals("route")) {
                    index++;
                    while (index < line.size() && !line.value(index).equals("ground") && !line.value(index).equals("global")) {
                        int comma = line.indexOf(index, ",");
                        if (comma < 0) {
                            throw line.refusal(index, "a route is 'route x,y x,y …'");
                        }
                        int yEnd = comma + 1 < line.size() && line.value(comma + 1).equals("-") ? comma + 3 : comma + 2;
                        route.add(new Point(Values.quantity(line, index, comma), Values.quantity(line, comma + 1, yEnd)));
                        index = yEnd;
                    }
                } else {
                    throw line.refusal(index, "a net line is 'name [ground] [global] [route x,y …]'");
                }
            }
            return new Net(line.value(0), ground, global, route);
        }).toList());
    }

    Block.Ports ports() {
        return new Block.Ports(reading.blockLines("ports").stream().map(line -> {
            if (line.size() != 2) {
                throw line.refusal("a port is 'input|output|power|ground|analog net'");
            }
            try {
                return new Port(PortDirection.valueOf(line.value(0).toUpperCase(Locale.ROOT)), line.value(1));
            } catch (IllegalArgumentException unknown) {
                throw line.refusal("'" + line.value(0) + "' is not a port direction: input, output, power, ground or analog");
            }
        }).toList());
    }

    /** The channels, and at most one {@code time 5 ms/div}: the time base the oscilloscope opens on. */
    Block.Probes probes() {
        List<Probe> probes = new ArrayList<>();
        Optional<Quantity> timePerDivision = Optional.empty();
        for (Reading.Line line : reading.blockLines("probes")) {
            if (!line.value(0).equals(TIME_BASE)) {
                probes.add(probe(line));
                continue;
            }
            if (timePerDivision.isPresent()) {
                throw line.refusal("the oscilloscope has one time base; this block names it twice");
            }
            timePerDivision = Optional.of(timeBase(line));
        }
        return new Block.Probes(probes, timePerDivision);
    }

    /** {@code time 5 ms/div}: seconds a division, positive. */
    private static Quantity timeBase(Reading.Line line) {
        int perDivision = line.indexOf(1, "/");
        if (line.size() < 2 || perDivision < 2 || !line.value(perDivision + 1).equals("div")) {
            throw line.refusal("the time base is written as time a division: 'time 5 ms/div'");
        }
        Quantity quantity = Values.written(line, 1, perDivision).quantity()
                .orElseThrow(() -> line.refusal(1, "the time base is a time: 'time 5 ms/div'"));
        if (!(quantity.value() > 0)) {
            throw line.refusal(1, "the time base is a positive time a division");
        }
        return quantity;
    }

    /** The word that opens the oscilloscope's time base in {@code probes}; no channel is named so. */
    private static final String TIME_BASE = "time";

    /**
     * {@code CH1 out 2 V/div} — a node's voltage —, {@code CH2 out against reference}, {@code CH3 current R1} — a current
     * clamp on a part's lead —, {@code L1 level clock}, {@code CH1 out 1 V/div shows X * pi / 2} — what the screen draws
     * instead of the reading —: the
     * oscilloscope's channels and the logic levels, named as the circuit names its nodes. A meter is no probe: it is a
     * part, wired across what it reads or in the break of a wire, and a line written as one is refused saying so.
     */
    private Probe probe(Reading.Line line) {
        if (line.size() < 2) {
            throw line.refusal("a probe is 'ID node [against node] [scale]', 'ID current part', 'ID power part' or 'ID level node'");
        }
        if (line.value(2).equals("(")) {
            throw line.refusal(1, "a probe names what it measures: 'CH1 out', 'CH2 out against reference', "
                    + "'A1 current R1', 'M1 power RL', 'L1 level clock'");
        }
        char measure = switch (line.value(1)) {
            case "current" -> 'I';
            case "power" -> 'P';
            case "level" -> 'L';
            default -> 'V';
        };
        // A meter is a part of the circuit now, wired as on a bench; this block holds the oscilloscope's channels alone.
        // An oscilloscope's channel may still take a CURRENT probe — a clamp around a part's lead, `CH2 current R1
        // 10 mA/div` —: that is the scope's own probe, not a meter, and it is what shows a current lagging a voltage.
        if (measure == 'I' && !line.value(0).matches("CH[0-9]+")) {
            throw line.refusal(1, "a meter is a part: an ammeter goes in the break of a wire in 'parts' — "
                    + "'" + line.value(0) + " ammeter (in = a, out = b)' —; 'probes' holds the oscilloscope's channels");
        }
        if (measure == 'P') {
            throw line.refusal(1, "a meter is a part: '" + line.value(0) + " wattmeter (in = a, out = b, plus = b, minus = 0)' "
                    + "in 'parts'; 'probes' holds the oscilloscope's channels");
        }
        if (measure == 'V' && (line.value(1).equals("voltage") || !line.value(0).matches("CH[0-9]+"))) {
            throw line.refusal(0, "a voltmeter is a part: '" + line.value(0) + " voltmeter (plus = a, minus = b)' in 'parts'; "
                    + "'probes' holds the oscilloscope's channels, CH1 to CH4");
        }
        int at = measure == 'V' ? 1 : 2;
        if (at >= line.size()) {
            throw line.refusal("'" + line.value(1) + "' names what it reads: '" + line.value(1) + " R1'");
        }
        int targetEnd = line.joinedEnd(at);
        String target = line.joinedWord(at);
        Optional<String> reference = Optional.empty();
        int next = targetEnd;
        if (line.value(next).equals("against")) {
            if (measure != 'V' || next + 1 >= line.size()) {
                throw line.refusal(next, "a voltage is read 'against' another node: 'CH2 out against reference'");
            }
            reference = Optional.of(line.value(next + 1));
            next += 2;
        }
        int last = line.size();
        Optional<String> expression = Optional.empty();
        int shows = line.indexOf(next, EXPRESSION_WORD);
        if (shows >= 0 && shows < last) {
            if (shows + 1 >= last) {
                throw line.refusal(shows, "'shows' is followed by what the screen draws, X being the reading: 'shows X * pi / 2'");
            }
            expression = Optional.of(line.text(shows + 1).text());
            last = shows;
        }
        if (last > next && DISPLAY_MODES.contains(line.value(last - 1))) {
            throw line.refusal(last - 1, "a channel has no display mode; a meter's display is chosen on the bench");
        }
        Optional<String> mode = Optional.empty();
        Optional<Quantity> scale = Optional.empty();
        if (next < last) {
            int perDivision = line.indexOf(next, "/");
            int end = perDivision >= 0 && perDivision < last && line.value(perDivision + 1).equals("div") ? perDivision : last;
            scale = Values.written(line, next, end).quantity();
        }
        return new Probe(line.value(0), measure, target, reference, scale, mode, expression);
    }

    /** The word after a channel's scale that opens what its screen shows instead of the reading. */
    private static final String EXPRESSION_WORD = "shows";

    /** What a meter's display showed when meters were probes: refused on a channel, which has none. */
    private static final java.util.Set<String> DISPLAY_MODES = java.util.Set.of("dc", "ac", "rms");

    // ─── single-line blocks ─────────────────────────────────────────────────────────────────────────

    /** {@code analysis transient for 5 ms}, {@code analysis ac from 10 Hz to 1 MHz points 200}. */
    Block.Analysis analysis(Token keyword) {
        Reading.Line line = reading.line();
        if (line.isEmpty()) {
            throw reading.refusal(keyword, "an analysis is 'analysis transient|ac|operating|dc_sweep [settings]'");
        }
        AnalysisKind kind;
        try {
            kind = AnalysisKind.valueOf(line.value(0).toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException unknown) {
            throw line.refusal("'" + line.value(0) + "' is not an analysis: transient, ac, operating or dc_sweep");
        }
        Map<String, SourceText> settings = new LinkedHashMap<>();
        List<String> keys = List.of("for", "from", "to", "points", "source", "step");
        int index = 1;
        while (index < line.size()) {
            String key = line.value(index);
            if (!keys.contains(key)) {
                throw line.refusal(index, "'" + key + "' is not an analysis setting: for, from, to, points, source or step");
            }
            int end = index + 1;
            while (end < line.size() && !keys.contains(line.value(end))) {
                end++;
            }
            settings.put(key, line.text(index + 1, end));
            index = end;
        }
        return new Block.Analysis(kind, settings);
    }

    /** {@code sheet 1200 x 760 grid 20}. */
    Block.Sheet sheet(Token keyword) {
        Reading.Line line = reading.line();
        int by = line.indexOf(0, "x");
        int gridAt = line.indexOf(0, "grid");
        if (by < 0) {
            throw reading.refusal(keyword, "a sheet is 'sheet width x height [grid step]'");
        }
        double width = Values.quantity(line, 0, by);
        double height = Values.quantity(line, by + 1, gridAt < 0 ? line.size() : gridAt);
        double grid = gridAt < 0 ? 20 : Values.quantity(line, gridAt + 1, line.size());
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
        reading.expect(BasicToken.T_OPEN_CURLY, "'{' to open the expectation");
        List<Part> bench = List.of();
        String sharedBench = null;
        double duration = 0;
        Double temperature = null;
        boolean realistic = false;
        List<String> measure = List.of();
        Criterion criterion = null;
        while (true) {
            reading.skipBlank();
            if (reading.atEnd()) {
                throw reading.refusal(keyword, "expectation '" + reading.literal(title) + "' is never closed");
            }
            if (reading.cursor.consumeIf(BasicToken.T_CLOSE_CURLY)) {
                break;
            }
            if (reading.atWord("bench")) {
                reading.cursor.next();
                Reading.Line header = reading.line();
                int start = 0;
                if (!header.isEmpty() && !header.value(0).equals("for")) {
                    // A shared bench named first: 'bench i2c-bus for 1 ms { … }' adds these parts to it.
                    sharedBench = header.joinedWord(0);
                    start = header.joinedEnd(0);
                }
                if (header.size() > start) {
                    if (!header.value(start).equals("for")) {
                        throw header.refusal("a bench is 'bench [shared] [for duration] [at temperature] [realistic] { parts }'");
                    }
                    // A realistic bench is the line's last word: its instruments load what they read.
                    int end = header.size();
                    if (header.value(end - 1).equals("realistic")) {
                        realistic = true;
                        end--;
                    }
                    int at = header.indexOf(start + 1, "at");
                    duration = Values.quantity(header, start + 1, at < 0 || at >= end ? end : at);
                    if (at >= 0 && at < end) {
                        temperature = Values.quantity(header, at + 1, end);
                    }
                }
                bench = partLines("bench");
            } else if (reading.atWord("measure")) {
                reading.cursor.next();
                measure = reading.line().words();
            } else {
                criterion = criterion(reading.line());
            }
        }
        if (measure.isEmpty() || criterion == null) {
            throw reading.refusal(keyword, "expectation '" + reading.literal(title)
                    + "' needs a measure and a criterion (near, within or is)");
        }
        return new Block.Expectation(reading.literal(title), reference, bench, sharedBench, duration, temperature, realistic, measure, criterion,
                reading.lineOf(keyword));
    }

    /** {@code bench i2c-bus { parts }}: parts written once, for every expectation that names the bench. */
    Block.Bench sharedBench(Token keyword) {
        Reading.Line header = reading.line();
        if (header.isEmpty() || header.joinedEnd(0) != header.size()) {
            throw reading.refusal(keyword, "a shared bench is 'bench name { parts }': 'bench i2c-bus { V1 …; R1 … }'");
        }
        return new Block.Bench(header.joinedWord(0), partLines("bench"));
    }

    /** {@code near 6.86 kHz +- 5%}, {@code within 26 V .. 30 V}, {@code is 1}. */
    Criterion criterion(Reading.Line line) {
        String kind = line.value(0);
        switch (kind) {
            case "near" -> {
                int plus = -1;
                for (int index = 1; index + 1 < line.size(); index++) {
                    if (line.value(index).equals("+") && line.value(index + 1).equals("-")) {
                        plus = index;
                        break;
                    }
                }
                if (plus < 0) {
                    throw line.refusal("'near' is 'near typical +- tolerance'");
                }
                boolean relative = line.value(line.size() - 1).equals("%");
                return new Criterion(CriterionKind.NEAR, Optional.of(Values.written(line, 1, plus).amount()),
                        Optional.of(Values.written(line, plus + 2, line.size()).amount()), relative,
                        Optional.empty(), Optional.empty(), null);
            }
            case "within" -> {
                int dots = line.indexOf(1, "..");
                if (dots < 0) {
                    throw line.refusal("'within' is 'within minimum .. maximum'");
                }
                return new Criterion(CriterionKind.WITHIN, Optional.empty(), Optional.empty(), false,
                        dots == 1 ? Optional.empty() : Optional.of(Values.written(line, 1, dots).amount()),
                        dots == line.size() - 1 ? Optional.empty() : Optional.of(Values.written(line, dots + 1, line.size()).amount()),
                        null);
            }
            case "is" -> {
                return new Criterion(CriterionKind.IS, Optional.empty(), Optional.empty(), false, Optional.empty(),
                        Optional.empty(), String.join("", line.words(1, line.size())));
            }
            default -> throw line.refusal("'" + kind + "' is not a criterion: near, within or is");
        }
    }
}
