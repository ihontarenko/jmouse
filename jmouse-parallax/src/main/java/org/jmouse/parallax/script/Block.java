package org.jmouse.parallax.script;

import org.jmouse.script.el.node.ScriptNode;
import org.jmouse.units.Quantity;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.ToDoubleFunction;

/**
 * One block of a jMX document, typed. Expressions stay {@link SourceText} — the language reads structure, and jMF
 * compiles the mathematics where the names it calls are known.
 */
public sealed interface Block {

    /** The block's name as written: {@code parameters}, {@code equations}. */
    String name();

    /** An unmodifiable copy that keeps the order the entries were written in. */
    private static <K, V> Map<K, V> orderedCopy(Map<K, V> entries) {
        return Collections.unmodifiableMap(new LinkedHashMap<>(entries));
    }

    // ─── shared by every computable kind ─────────────────────────────────────────────────────────────

    /** {@code a .. b}: both bounds as written. */
    record Range(SourceText minimum, SourceText maximum) {
    }

    /** What a parameter is besides its value; {@code EXACT} keeps an integer as written, to any number of digits. */
    /**
     * What a parameter is besides its value. {@code TEXT} is not written as a word: a value in quotes —
     * {@code first_line = "Hello"} — makes it, and a text has no range, step or unit.
     */
    enum ParameterFlag { LOG, INTEGER, ANIMATE, LIVE, PER_UNIT, EXACT, TEXT }

    /**
     * {@code R = 10 kOhm in 100 Ohm .. 1 MOhm log "resistance"}.
     *
     * @param value    the value as written, without its unit
     * @param quantity the value in SI with its unit, when it is a literal; empty for an expression ({@code 8 / 3})
     * @param values   the values a slider stops at, when it lists them as a jME list — {@code N = 7 in [1, 3, 7, 19, 49]}; its range is
     *                 then the first to the last
     */
    record Parameter(String name, SourceText value, Optional<Quantity> quantity, Optional<Range> range,
                     Optional<SourceText> step, Set<ParameterFlag> flags, String description, List<SourceText> values) {
        public Parameter {
            flags = Set.copyOf(flags);
            values = values == null ? List.of() : List.copyOf(values);
        }

        public Parameter(String name, SourceText value, Optional<Quantity> quantity, Optional<Range> range,
                         Optional<SourceText> step, Set<ParameterFlag> flags, String description) {
            this(name, value, quantity, range, step, flags, description, List.of());
        }

        public boolean has(ParameterFlag flag) {
            return flags.contains(flag);
        }
    }

    record Parameters(List<Parameter> parameters) implements Block {
        public Parameters {
            parameters = List.copyOf(parameters);
        }

        public String name() {
            return "parameters";
        }
    }

    /**
     * {@code x in -3 .. 3}, {@code n in 0 .. 500 integer}, {@code (z) in -2 - 2 i .. 2 + 2 i}, {@code t in 0 .. 2 pi
     * periodic} — periodic: the end of the range is its start again, as on a closed curve.
     *
     * @param complex one name in parentheses, {@code (z)}: a complex input over the rectangle between two corners;
     *                several names in parentheses, {@code (x, y)}, are real inputs sharing one range
     */
    record Input(List<String> names, Optional<Range> range, Optional<SourceText> step, boolean integer, boolean periodic,
                 boolean complex, String description) {
        public Input {
            names = List.copyOf(names);
        }
    }

    record Inputs(List<Input> inputs) implements Block {
        public Inputs {
            inputs = List.copyOf(inputs);
        }

        public String name() {
            return "inputs";
        }
    }

    /** What an output is: the type the interface draws it as. */
    enum OutputType { SCALAR, SERIES, CURVE, SURFACE, VOLUME, FIELD, IMAGE, CLOUD, PATH, MESH, MATRIX, TABLE, SIGNAL, LOGIC,
        /** A curve or a surface whose integral is one: a probability density, sampled and integrated by its engine. */
        DENSITY }

    /**
     * {@code z : surface = A sin(k x)}, {@code trace : curve = (sin(t), cos(t))}, {@code boundary : surface where …},
     * or {@code estimate : series} when a {@code run} block or an engine writes it. A parametric surface may say how
     * its parameter square is glued: {@code shape : surface glued torus = (…)}.
     *
     * @param components one expression, or one per coordinate of a tuple; empty for a written-elsewhere output
     * @param equation   the {@code where} equation of an implicit output
     */
    record Output(String name, Optional<OutputType> type, Optional<String> gluing, List<SourceText> components,
                  Optional<SourceText> equation, String description) {
        public Output {
            components = List.copyOf(components);
        }
    }

    record Outputs(List<Output> outputs) implements Block {
        public Outputs {
            outputs = List.copyOf(outputs);
        }

        public String name() {
            return "outputs";
        }
    }

    /** {@code formulas { f(u) = u^2 - 1 }} — jMF definitions as written. A shared library is pulled in with {@code use}. */
    record Formulas(Optional<SourceText> definitions) implements Block {

        public String name() {
            return "formulas";
        }
    }

    /**
     * One state: {@code x = 1 continuous}, {@code stored = X discrete}, or a row of them — {@code memory[256] = 0
     * discrete}, every slot starting at that value, read with {@code item(memory, i)} and written with {@code with}. A
     * discrete state is written only by behaviour; an initial value may be {@code X}.
     *
     * @param length how many slots a row has; 0 for a single value
     */
    record StateVariable(String name, SourceText initial, boolean discrete, Optional<Range> range, String description, int length) {
        public StateVariable {
            if (length < 0) {
                throw new IllegalArgumentException("A row of states cannot have a negative length.");
            }
        }
    }

    record State(List<StateVariable> variables) implements Block {
        public State {
            variables = List.copyOf(variables);
        }

        public String name() {
            return "state";
        }
    }

    /** Which form an equation is written in. */
    enum EquationForm {
        /** {@code d(x)/dt = …} */
        DERIVATIVE,
        /** {@code x_next = …} */
        NEXT,
        /** {@code x_start = …} */
        START,
        /** {@code current a -> b = …} */
        CURRENT,
        /** {@code voltage a -> b = …} */
        VOLTAGE,
        /** {@code 0 = …} */
        IMPLICIT,
        /** {@code local name = …} */
        LOCAL
    }

    /**
     * @param subject the variable of a derivative, a step or a start, or a local's name
     * @param positive the first node of a current or a voltage
     * @param negative the second node of a current or a voltage
     */
    /**
     * One line of {@code equations}.
     *
     * @param type what a {@code let} names its value as — {@code let I_through : current = …} —; null for every other
     *             form and for a {@code let} written without one
     */
    record Equation(EquationForm form, String subject, String positive, String negative, SourceText expression,
                    org.jmouse.el.type.ValueType type) {

        public Equation(EquationForm form, String subject, String positive, String negative, SourceText expression) {
            this(form, subject, positive, negative, expression, null);
        }
    }

    record Equations(List<Equation> equations) implements Block {
        public Equations {
            equations = List.copyOf(equations);
        }

        public String name() {
            return "equations";
        }
    }

    /**
     * {@code heat { d(heating)/dt = (power * thermal_resistance - heating) / thermal_time_constant; temperature = ambient
     * + heating }}: how a part warms — the law of its own temperature, as physics, not as code. Each {@code d(x)/dt} is
     * a thermal state that starts at nought; {@code let} names a value; {@code temperature = …} is the part's own
     * temperature, kelvin, which its equations read as {@code temperature}. It reads {@code power} — what the part's
     * equations dissipate, which the engine derives — {@code ambient}, the bench's temperature, and the part's
     * parameters. The engine advances it after every accepted step, outside Newton, as slow as heat is.
     *
     * @param lines       the rates and the lets, in the order written
     * @param temperature the right side of {@code temperature = …}
     */
    record Heat(List<Equation> lines, SourceText temperature) implements Block {
        public Heat {
            lines = List.copyOf(lines);
        }

        public String name() {
            return "heat";
        }
    }

    /**
     * {@code image { over X in a .. b, Y in c .. d; start u = …; stop when …; hit when … crosses 0 where …; value = … }}:
     * a picture in which every pixel is one run of the system — started from the pixel, stepped until it stops or hits,
     * and shaded by what it hit.
     *
     * @param starts   each variable's start as an expression of the pixel and the parameters
     * @param stops    conditions, any of which ends a run unhit (a comparison is 1 or 0)
     * @param crossing what crossing zero marks a hit
     * @param where    a condition the hit must also meet, else the run goes on
     * @param value    the pixel's value at the hit; a run that never hits is 0
     */
    record Image(String horizontal, Range horizontalRange, String vertical, Range verticalRange,
                 Map<String, SourceText> starts, List<SourceText> stops, SourceText crossing,
                 Optional<SourceText> where, SourceText value) implements Block {
        public Image {
            starts = orderedCopy(starts);
            stops = List.copyOf(stops);
        }

        public String name() {
            return "image";
        }
    }

    /**
     * {@code bench i2c-bus { parts }}: parts a component's expectations share — a supply, the bus's pull-ups —, written
     * once; inherited by the parts that extend it.
     */
    record Bench(String title, List<Part> parts) implements Block {
        public Bench {
            parts = List.copyOf(parts);
        }

        public String name() {
            return "bench";
        }
    }

    /**
     * {@code behaviour { … }}: jMS handlers and functions, parsed by jMS.
     *
     * @param source the body between the braces as written: what the document is written back with
     */
    record Behaviour(ScriptNode script, SourceText source) implements Block {
        public String name() {
            return "behaviour";
        }
    }

    /** {@code run { … }}: a jMS program that writes the declared outputs. */
    record Run(ScriptNode script, SourceText source) implements Block {
        public String name() {
            return "run";
        }
    }

    /**
     * {@code type power_density = power / area}: a quantity the document names, built from the known ones, which a
     * {@code let} after it may be typed by. Kept where it stands among the blocks, since only what follows reads it.
     *
     * @param definition the known types it is made of, as written: joined by {@code *} and {@code /}, with whole powers
     */
    record TypeDefinition(String type, SourceText definition) implements Block {
        public String name() {
            return "type";
        }
    }

    // ─── view: the eXperience ────────────────────────────────────────────────────────────────────────

    sealed interface ViewItem {
    }

    /** {@code surface primary resolution 120}, {@code section along y = 0}: the words after the name as written. */
    record Representation(String representation, boolean primary, SourceText settings) implements ViewItem {
    }

    /**
     * {@code preset "Calm" "a slow swell" { A = 0.3, k = 0.5 }}; a {@code camera …} line inside the braces is where
     * the preset looks from.
     */
    record Preset(String title, String description, Map<String, SourceText> values, Optional<Camera> camera)
            implements ViewItem {
        public Preset {
            values = orderedCopy(values);
        }
    }

    /** {@code camera azimuth 45 elevation 30 target [0, 0, 1]}: each setting's value as written. */
    record Camera(Map<String, SourceText> settings) implements ViewItem {
        public Camera {
            settings = orderedCopy(settings);
        }
    }

    /** {@code explain k "…"}: a hint shown beside a control. */
    record Explanation(String subject, String text) implements ViewItem {
    }

    /** {@code without play}: a capability the structure gives and the author withholds. */
    record Without(Set<Capability> capabilities) implements ViewItem {
        public Without {
            capabilities = Set.copyOf(capabilities);
        }
    }

    record View(List<ViewItem> items) implements Block {
        public View {
            items = List.copyOf(items);
        }

        public String name() {
            return "view";
        }
    }

    // ─── checks ──────────────────────────────────────────────────────────────────────────────────────

    /**
     * What a measured value must be: near a typical value, between limits, or exactly a value. The values are
     * {@link Amount}s — {@code near 6.86 kHz +- 5%} and {@code near R * C +- 2%} alike — and a compiler turns them into
     * numbers against the document's parameters with {@link #bounds}.
     *
     * @param typical   a {@code near} criterion's typical value
     * @param tolerance a {@code near} criterion's tolerance; relative when {@code relative}
     * @param minimum   a {@code within} criterion's lower limit; empty for none
     * @param maximum   a {@code within} criterion's upper limit; empty for none
     * @param exact     an {@code is} criterion's value, as written
     */
    record Criterion(CriterionKind kind, Optional<Amount> typical, Optional<Amount> tolerance, boolean relative,
                     Optional<Amount> minimum, Optional<Amount> maximum, String exact) {

        /** The interval a {@code near} or {@code within} criterion admits, its amounts evaluated by {@code value}. */
        public double[] bounds(ToDoubleFunction<Amount> value) {
            return switch (kind) {
                case NEAR -> {
                    double centre = value.applyAsDouble(typical.orElseThrow());
                    double spread = Math.abs(value.applyAsDouble(tolerance.orElseThrow()));
                    double width = relative ? Math.abs(centre) * spread : spread;
                    yield new double[] {centre - width, centre + width};
                }
                case WITHIN -> new double[] {
                        minimum.map(value::applyAsDouble).orElse(Double.NEGATIVE_INFINITY),
                        maximum.map(value::applyAsDouble).orElse(Double.POSITIVE_INFINITY)};
                case IS -> throw new IllegalStateException("an 'is' criterion has no interval");
            };
        }
    }

    enum CriterionKind { NEAR, WITHIN, IS }

    /**
     * {@code expect "astable frequency" from "datasheet" { bench for 3 ms { … } measure … near … }}.
     *
     * @param bench       the bench's parts, in the same notation as a circuit's
     * @param temperature the bench's, in °C — {@code bench for 1 ms at 85 °C} —; null for 25 °C
     * @param realistic   whether the bench is a realistic one — its instruments load what they read, as real ones do
     * @param measure     the words of the measurement, as written
     */
    /**
     * {@code expect …}: its bench — the parts it writes, over the shared bench it names ({@code bench i2c-bus for 1 ms})
     * when it names one —, the measurement and what it must come to.
     *
     * @param sharedBench the component's or its base's {@code bench} it stands on; null for a bench of its own alone
     */
    record Expectation(String title, String reference, List<Part> bench, String sharedBench, double duration,
                       Double temperature, boolean realistic, List<String> measure, Criterion criterion, int line)
            implements Block {
        public Expectation {
            bench = List.copyOf(bench);
            measure = List.copyOf(measure);
        }

        public String name() {
            return "expect";
        }
    }

    // ─── components ──────────────────────────────────────────────────────────────────────────────────

    enum PinNature { ANALOG, DIGITAL, POWER, GROUND }

    /** Which way a pin works: {@code input} reads its line, {@code output} drives it, {@code bidirectional} does both. */
    enum PinDirection { INPUT, OUTPUT, BIDIRECTIONAL }

    /**
     * {@code out analog output 1 7 "output"}: the package numbers per unit, its nature, its direction, and the
     * remaining words ({@code edge}, {@code bubble}, {@code open-drain}).
     */
    record Pin(String name, List<Integer> numbers, PinNature nature, PinDirection direction, List<String> attributes,
               String description) {
        public Pin {
            numbers = List.copyOf(numbers);
            attributes = List.copyOf(attributes);
        }
    }

    /**
     * {@code q[0..7] digital output 15, 1, 2, 3, 4, 5, 6, 7}: pins that are one value, written once — each a pin of its
     * own ({@code q_0} … {@code q_7}), and together a number a behaviour reads and drives at once, its first pin the
     * least significant bit.
     *
     * @param pins its pins, in the order of its bits
     */
    record Bus(String name, List<String> pins) {
        public Bus {
            pins = List.copyOf(pins);
        }
    }

    /** Every pin, the bits of each bus among them, and the buses they make. */
    record Pins(List<Pin> pins, List<Bus> buses) implements Block {
        public Pins {
            pins = List.copyOf(pins);
            buses = List.copyOf(buses);
        }

        public Pins(List<Pin> pins) {
            this(pins, List.of());
        }

        public String name() {
            return "pins";
        }
    }

    record InternalNode(String name, boolean digital, String description) {
    }

    record Nodes(List<InternalNode> nodes) implements Block {
        public Nodes {
            nodes = List.copyOf(nodes);
        }

        public String name() {
            return "nodes";
        }
    }

    /** {@code pin out at 50, 0 "out"}. */
    record SymbolPin(String pin, double x, double y, String label) {
    }

    /** {@code label value at 0, -12}: where a part's value or name is printed, the text centred on the point. */
    record SymbolLabel(String subject, double x, double y) {
    }

    /** {@code indicator level from -4, -14 to 4, 14}: where a part's live figure of that name is drawn on its symbol. */
    record SymbolIndicator(String name, double left, double top, double right, double bottom) {
    }

    /**
     * {@code input closed}, {@code input current 0 .. 20 mA}: a figure a symbol is drawn from, which the component that
     * uses the symbol binds ({@code symbol uses switch-symbol { closed = pressed }}); 0 … 1 unless a range is written.
     * {@code input screen picture} is a picture — a row of bytes a {@code bitmap} draws —, not a number.
     *
     * @param range   the range as written, empty for the default 0 … 1 and for a picture
     * @param picture whether it is a picture
     */
    record SymbolInput(String name, double low, double high, Optional<SourceText> range, boolean picture) {
    }

    /**
     * How a value of a drawing changes: {@code STILL} never; {@code LIVE} — written {@code live} — with the circuit, as
     * it reads the symbol's inputs; {@code ANIMATED} — written {@code animate} — in time by itself, on the wall clock.
     */
    enum SymbolMotion {
        STILL, LIVE, ANIMATED
    }

    /**
     * {@code live rotate = between(-30 deg, 0 deg, smooth(closed, 60 ms))}: one property of a drawn element and the
     * values it is written as — {@code move = 0, between(-6, 0, pressed)} is two. Each value is the text as written and
     * the same text in jMF, quantities in SI ({@code 60 ms} → {@code 0.06}) and {@code ->} as a comma.
     */
    record SymbolProperty(String name, SourceText written, List<String> formulas, SymbolMotion motion) {
        public SymbolProperty {
            formulas = List.copyOf(formulas);
        }
    }

    /**
     * One element of a drawing: a {@code path}, {@code box}, {@code circle} or {@code line}, a {@code group} of
     * elements, or a {@code when} layer shown while its condition holds. A plain unnamed {@code path "…"} is not one —
     * it stays in {@link Symbol#paths()}.
     *
     * @param shape      {@code path}, {@code box}, {@code circle}, {@code line}, {@code group} or {@code when}
     * @param name       how it is addressed ({@code hide halo}), empty when it has none
     * @param live       whether it was written {@code live}: something in it changes
     * @param path       the SVG path data of a {@code path}, empty otherwise
     * @param geometry   its points and sizes by word: {@code at → [x, y]}, {@code size → [w, h]}, {@code radius → [r]},
     *                   {@code from}, {@code to}, {@code pivot}
     * @param condition  a {@code when}'s condition, as a property named {@code when}; null for any other element
     * @param properties what it is drawn with, in the order written
     * @param hidden     the elements a {@code when} hides while it holds
     * @param children   the elements of a {@code group} or a {@code when}
     */
    record SymbolElement(String shape, String name, boolean live, String path, Map<String, List<Double>> geometry,
                         SymbolProperty condition, List<SymbolProperty> properties, List<String> hidden,
                         List<SymbolElement> children, int line) {
        public SymbolElement {
            geometry = orderedCopy(geometry);
            properties = List.copyOf(properties);
            hidden = List.copyOf(hidden);
            children = List.copyOf(children);
        }
    }

    /**
     * {@code display { level = terminal_one - terminal_two }}: what a component shows on its symbol while it runs — each a
     * jMF expression over its pins' voltages, its parameters and {@code power}, drawn in the symbol's indicator of
     * that name. A figure with a unit is written beside the part, followed by its caption when it has one: what the
     * figure is of — {@code charge = 100 * (terminal_one - terminal_two) / voltage_rating unit % "of the rated voltage"}.
     */
    record Display(Map<String, SourceText> figures, Map<String, String> units,
                   Map<String, String> captions) implements Block {
        public Display {
            figures = orderedCopy(figures);
            units = units == null ? Map.of() : orderedCopy(units);
            captions = captions == null ? Map.of() : orderedCopy(captions);
        }

        public String name() {
            return "display";
        }
    }

    /**
     * {@code measure { reading = plus - minus unit V }}: what an instrument reads — each a name, a jMF expression and the
     * unit it comes out in. The expression reads what the component's own equations read — each pin's name is its
     * voltage, {@code current(pin)} the current into a pin, its states, its parameters, {@code power} and {@code time} —,
     * so a voltmeter, an ammeter, a frequency counter or a meter nobody has thought of yet is written in its document and
     * known to no engine. The block is what makes a component an instrument; how much it loads what it measures is its
     * equations, which may read the bench's {@code realistic}.
     */
    record Measure(Map<String, MeasureFigure> figures) implements Block {
        public Measure {
            figures = orderedCopy(figures);
        }

        public String name() {
            return "measure";
        }
    }

    /**
     * {@code leads { plus red stripes black;  minus black stripes white }}: how an instrument's leads look on a sheet — each
     * pin's lead in a colour, optionally striped in another. A look, nothing else: what is measured is {@code measure}'s.
     */
    record Leads(Map<String, LeadLook> looks) implements Block {
        public Leads {
            looks = orderedCopy(looks);
        }

        public String name() {
            return "leads";
        }
    }

    /** A lead's colour, and the colour of its stripes or nothing; both from {@link #LEAD_COLOURS}. */
    record LeadLook(String colour, String stripes) {
    }

    /** The colours a lead can be: the colours of real test leads. */
    List<String> LEAD_COLOURS = List.of("red", "black", "white", "green", "blue", "yellow", "grey");

    /**
     * One figure of a {@code measure} block: what it reads, in what unit, and whether it is a signal — a voltage, a
     * current, a power that a bench reads as DC, AC or RMS — rather than a figure the instrument worked out, such as a
     * frequency or a duty, which is read as it is.
     */
    record MeasureFigure(SourceText expression, String unit, boolean signal) {
    }

    /**
     * How a part is drawn. Either a drawing ({@code size}, {@code path}, {@code pin … at}, {@code label}), a shape
     * whose pins are placed by side ({@code box { left d, clk edge;  right q }}), or a reference to symbol documents
     * ({@code uses resistor-symbol { a terminal_one;  b terminal_two }}).
     *
     * @param uses   the symbol documents a component may be drawn with, the default first; empty for a shape or a
     *               drawing
     * @param ports  a referenced symbol's port → the component pin it is, as written; a port not named here is the pin
     *               of the same name
     * @param shape  {@code box} or {@code triangle}; empty for a drawing or a reference
     * @param sides  side → pins and their words, as written ({@code left → [d, clk edge]})
     * @param upright the paths that stay upright however the part is turned — a meter's letter —, written
     *               {@code path "…" upright}
     * @param inputs   a drawing's {@code input} lines: what it is drawn from
     * @param elements a drawing's named, shaped, grouped and conditional elements
     * @param bindings a reference's {@code input = expression} lines: what feeds each input of the symbols it uses —
     *                 a jMF expression over the part, read while it runs as a {@code display} figure is
     */
    record Symbol(List<String> uses, Map<String, String> ports, Optional<String> shape, Map<String, List<String>> sides,
                  double width, double height, List<String> paths, List<SymbolPin> pins, List<SymbolLabel> labels,
                  List<SymbolIndicator> indicators, List<String> upright, List<SymbolInput> inputs,
                  List<SymbolElement> elements, Map<String, SourceText> bindings)
            implements Block {
        public Symbol {
            uses = List.copyOf(uses);
            ports = orderedCopy(ports);
            sides = orderedCopy(sides);
            paths = List.copyOf(paths);
            pins = List.copyOf(pins);
            labels = List.copyOf(labels);
            upright = upright == null ? List.of() : List.copyOf(upright);
            inputs = inputs == null ? List.of() : List.copyOf(inputs);
            elements = elements == null ? List.of() : List.copyOf(elements);
            bindings = bindings == null ? Map.of() : orderedCopy(bindings);
        }

        public String name() {
            return "symbol";
        }
    }

    // ─── circuits: one netlist notation for circuits, component internals and benches ──────────────────

    /**
     * Where a part is drawn: {@code at x, y [rotate degrees] [mirror]}, and where its texts stand when they were put
     * somewhere by hand — {@code name at dx, dy}, {@code value at …}, {@code readings at …}, from the part's centre —,
     * and how an instrument's display reads, when the reader chose: {@code shows rms graph}.
     *
     * @param size how big a block — a placed sheet — is drawn, {@code size 240 x 160}, as KiCad sizes a hierarchical
     *             sheet; empty when the sheet sizes it from its ports and name
     */
    record Placement(double x, double y, double rotation, boolean mirrored, Map<String, Point> labels,
                     List<String> shows, Optional<Size> size) {
        public Placement {
            labels = labels == null ? Map.of() : orderedCopy(labels);
            shows = shows == null ? List.of() : List.copyOf(shows);
            size = size == null ? Optional.empty() : size;
        }

        public Placement(double x, double y, double rotation, boolean mirrored, Map<String, Point> labels,
                         List<String> shows) {
            this(x, y, rotation, mirrored, labels, shows, Optional.empty());
        }

        public Placement(double x, double y, double rotation, boolean mirrored) {
            this(x, y, rotation, mirrored, Map.of(), List.of());
        }
    }

    /** {@code 240 x 160}: a width and a height in sheet units. */
    record Size(double width, double height) {
    }

    /**
     * {@code R1 resistor (in, out) resistance = R symbol resistor-iec}. Where it is drawn is the {@code layout} block's.
     *
     * @param reference  the component it is: a slug, optionally pinned ({@code lm358@3})
     * @param unit       which unit of a multi-unit package, when named
     * @param positional pins bound in the component's pin order: {@code (in, out)}
     * @param named      pins bound by name: {@code (inp = ref, out = out)}
     * @param settings   parameter overrides and words such as {@code closed}
     * @param symbol     which of the component's symbols the part is drawn with, when not its default: one its
     *                   {@code symbol uses} line names
     */
    record Part(String id, String reference, Optional<String> unit, List<String> positional, Map<String, String> named,
                Map<String, SourceText> settings, Optional<String> symbol, int line) {
        public Part {
            positional = List.copyOf(positional);
            named = orderedCopy(named);
            settings = orderedCopy(settings);
        }
    }

    record Parts(List<Part> parts) implements Block {
        public Parts {
            parts = List.copyOf(parts);
        }

        public String name() {
            return "parts";
        }
    }

    record Point(double x, double y) {
    }

    /**
     * {@code out route 300,200 380,200}, {@code gnd ground}, {@code vcc global}: what belongs to a node itself. A
     * global net is one name across every sheet of a project — the rails a power symbol draws.
     */
    record Net(String net, boolean ground, boolean global, List<Point> route) {
        public Net {
            route = List.copyOf(route);
        }
    }

    /**
     * One thing on the sheet: {@code R1 at 240, 200 rotate 90} places a part; {@code GND1 ground at 600, 520} and
     * {@code J1 junction at 200, 200} are marks that are not parts. A ground mark is node 0 unless it names another:
     * {@code A1 ground agnd at 600, 520} is the net {@code agnd}, a ground of its own that the engine keeps apart from 0.
     *
     * <p>A part of several units may place each unit where it belongs: {@code U4.B at 1500, 2900} draws unit B there,
     * with the pins of that unit; the part's own line ({@code U4 at …}) draws the units not placed on their own, and the
     * pins every unit shares.</p>
     *
     * @param unit which unit of a multi-unit part the line places, written {@code U4.B}; empty for the part itself
     * @param mark {@code ground} or {@code junction} for a mark; empty for a part, which the {@code parts} block names
     * @param net  the net a ground mark is; empty for node 0
     */
    record LayoutItem(String id, Optional<String> unit, Optional<String> mark, Optional<String> net, Placement placement) {
        public LayoutItem(String id, Optional<String> mark, Optional<String> net, Placement placement) {
            this(id, Optional.empty(), mark, net, placement);
        }
    }

    /**
     * {@code W1 wire R1.negative to C1.positive via 240 120, 300 120}: a wire from one terminal to another, through its
     * corners. A terminal is a part's pin — {@code positive} and {@code negative} for a two-terminal part —, a ground's
     * {@code ground} or a junction's {@code j}.
     */
    record LayoutWire(String id, String from, String to, List<Point> bends) {
        public LayoutWire {
            bends = List.copyOf(bends);
        }
    }

    /** {@code layout { … }}: how a circuit is drawn — nothing in it changes what is computed. */
    record Layout(List<LayoutItem> items, List<LayoutWire> wires) implements Block {
        public Layout {
            items = List.copyOf(items);
            wires = List.copyOf(wires);
        }

        public String name() {
            return "layout";
        }
    }

    record Nets(List<Net> nets) implements Block {
        public Nets {
            nets = List.copyOf(nets);
        }

        public String name() {
            return "nets";
        }
    }

    enum PortDirection { INPUT, OUTPUT, POWER, GROUND, ANALOG }

    /** {@code input in}: a node that makes the circuit a component when it is used inside another. */
    record Port(PortDirection direction, String net) {
    }

    record Ports(List<Port> ports) implements Block {
        public Ports {
            ports = List.copyOf(ports);
        }

        public String name() {
            return "ports";
        }
    }

    /**
     * {@code CH1 out 2 V/div}, {@code CH2 out against ref}, {@code M2 current PS1.negative}, {@code M3 power RL rms},
     * {@code L1 level clock}.
     *
     * @param measure   {@code V} (voltage), {@code I} (current), {@code P} (power) or {@code L} (logic level)
     * @param target    a node, or a part — a current's with its terminal, `PS1.negative`
     * @param reference the second node of a differential voltage probe; ground when absent
     * @param mode      what a meter's display shows: {@code dc}, {@code ac} or {@code rms}
     * @param expression what an oscilloscope channel shows instead of its reading — {@code shows X * pi / 2}, X being
     *                   the reading —, computed by the screen
     */
    record Probe(String id, char measure, String target, Optional<String> reference, Optional<Quantity> scale,
                 Optional<String> mode, Optional<String> expression) {
    }

    /**
     * The oscilloscope as the circuit sets it up: its channels, and {@code time 5 ms/div} — the time base it opens on.
     *
     * @param timePerDivision seconds a division, when the circuit names one
     */
    record Probes(List<Probe> probes, Optional<Quantity> timePerDivision) implements Block {
        public Probes {
            probes = List.copyOf(probes);
            timePerDivision = timePerDivision == null ? Optional.empty() : timePerDivision;
        }

        public String name() {
            return "probes";
        }
    }

    enum AnalysisKind { TRANSIENT, AC, OPERATING, DC_SWEEP }

    /** {@code analysis transient for 5 ms}, {@code analysis ac from 10 Hz to 1 MHz points 200}. */
    record Analysis(AnalysisKind kind, Map<String, SourceText> settings) implements Block {
        public Analysis {
            settings = orderedCopy(settings);
        }

        public String name() {
            return "analysis";
        }
    }

    // ─── footprints: how a part sits on a board ───────────────────────────────────────────────────────

    /** What a pad is on the board: plated through it, on its surface, a connector's edge finger, or a bare hole. */
    enum PadType {
        THROUGH_HOLE("through-hole"), SURFACE("surface"), CONNECTOR("connector"), UNPLATED_HOLE("unplated-hole");

        private final String word;

        PadType(String word) {
            this.word = word;
        }

        public String word() {
            return word;
        }

        public static Optional<PadType> of(String word) {
            return java.util.Arrays.stream(values()).filter(type -> type.word.equals(word)).findFirst();
        }
    }

    /** A pad's copper outline. */
    enum PadShape {
        CIRCLE("circle"), RECTANGLE("rectangle"), OVAL("oval"), TRAPEZOID("trapezoid"),
        ROUNDED_RECTANGLE("rounded-rectangle"), CUSTOM("custom");

        private final String word;

        PadShape(String word) {
            this.word = word;
        }

        public String word() {
            return word;
        }

        public static Optional<PadShape> of(String word) {
            return java.util.Arrays.stream(values()).filter(shape -> shape.word.equals(word)).findFirst();
        }
    }

    /**
     * {@code pad "1" through-hole rectangle at 0, 0 size 1.905 x 2 drill 1.1}: one pad, in millimetres from the
     * footprint's origin, Y downwards as on the board.
     *
     * @param number      what the package calls it — {@code 1}, {@code A3}; the component's pin of that package number
     *                    lands on it; empty for a mounting hole
     * @param rotation    degrees, counter-clockwise as KiCad turns a pad
     * @param drillWidth  the hole's width; 0 for a pad on the surface
     * @param drillHeight the hole's height — an oval hole's other side —; equal to its width for a round one
     * @param roundness   a rounded rectangle's corner radius as a share of its smaller side
     */
    record FootprintPad(String number, PadType type, PadShape shape, double x, double y, double rotation, double width,
                        double height, double drillWidth, double drillHeight, double roundness) {
    }

    /**
     * {@code outline silkscreen width 0.12 "M-2.58 -3.27L…"}: the lines a footprint draws on one of the board's layers, as
     * path data in millimetres.
     *
     * @param layer {@code silkscreen} (printed on the board), {@code fabrication} (the part's body), {@code courtyard}
     *              (the room it keeps), {@code copper}
     */
    record FootprintOutline(String layer, double width, boolean filled, String path) {
    }

    /** A {@code footprint} document's lines: its pads and its outlines. */
    record Footprint(List<FootprintPad> pads, List<FootprintOutline> outlines) implements Block {
        public Footprint {
            pads = List.copyOf(pads);
            outlines = List.copyOf(outlines);
        }

        public String name() {
            return "footprint";
        }
    }

    /**
     * {@code footprint to-220-3-vertical}: the footprint a component's package lands on — every package pin number of
     * the component is one of its pads.
     */
    record FootprintUse(String footprint) implements Block {
        public String name() {
            return "footprint";
        }
    }

    /** {@code sheet 1200 x 760 grid 20}. */
    record Sheet(double width, double height, double grid) implements Block {
        public String name() {
            return "sheet";
        }
    }

    // ─── kinds with a structure of their own ─────────────────────────────────────────────────────────

    /** {@code 0.85 : (0.85 x + 0.04 y, -0.04 x + 0.85 y + 1.6)}. */
    record WeightedMap(SourceText weight, List<SourceText> components) {
        public WeightedMap {
            components = List.copyOf(components);
        }
    }

    record Maps(List<WeightedMap> maps) implements Block {
        public Maps {
            maps = List.copyOf(maps);
        }

        public String name() {
            return "maps";
        }
    }

    record Axiom(String axiom) implements Block {
        public String name() {
            return "axiom";
        }
    }

    /** {@code F -> "F+F--F+F"}. */
    record Rules(Map<String, String> rules) implements Block {
        public Rules {
            rules = orderedCopy(rules);
        }

        public String name() {
            return "rules";
        }
    }

    /** Lines of {@code name = value}: a turtle's angle and step. */
    record Turtle(Map<String, SourceText> settings) implements Block {
        public Turtle {
            settings = orderedCopy(settings);
        }

        public String name() {
            return "turtle";
        }
    }

    /** A construction's statements, as written: {@code O = intersection(side_ab, side_bc)}. */
    record Steps(List<SourceText> steps) implements Block {
        public Steps {
            steps = List.copyOf(steps);
        }

        public String name() {
            return "steps";
        }
    }

    /** {@code vertices = cartesian(-1, 1, dimension)}. */
    record Vertices(SourceText expression) implements Block {
        public String name() {
            return "vertices";
        }
    }

    /**
     * {@code edges where hamming(first, second) = 1}, or the pairs of vertex indices listed:
     * {@code edges = [[0, 1], [1, 2]]}.
     *
     * @param condition what joins two vertices, when the edges are derived
     * @param pairs     the list of index pairs, when the edges are written out
     */
    record Edges(Optional<SourceText> condition, Optional<SourceText> pairs) implements Block {
        public String name() {
            return "edges";
        }
    }

    /** {@code points { (0, 0), (1, 2) }}: each point's coordinates. */
    record PointList(List<List<SourceText>> points) implements Block {
        public PointList {
            points = points.stream().map(List::copyOf).toList();
        }

        public String name() {
            return "points";
        }
    }

    // ─── catalogue ───────────────────────────────────────────────────────────────────────────────────

    /** {@code items lorenz, rossler}: what a collection holds. */
    record Items(List<String> references) implements Block {
        public Items {
            references = List.copyOf(references);
        }

        public String name() {
            return "items";
        }
    }

    /**
     * {@code section "Ordinary" { items line, parabola }}: a titled part of a collection, its references in order. A
     * collection may hold sections, top-level {@code items}, or both.
     */
    record Section(String title, List<String> references) implements Block {
        public Section {
            references = List.copyOf(references);
        }

        public String name() {
            return "section";
        }
    }

    /**
     * {@code lesson "The road to chaos" as logistic-road-to-chaos { … }}: one lesson of a course and its steps. Its
     * place in the course is the order it is written in.
     *
     * @param slug    the stable address: {@code as slug} when written, otherwise derived from the title
     * @param summary one line under the title, when written
     * @param minutes how long it takes, when written: {@code minutes 8}
     * @param object  the object the lesson is about: {@code object wave}
     */
    record Lesson(String title, String slug, Optional<String> summary, Optional<Integer> minutes,
                  Optional<String> object, List<CourseStep> steps) implements Block {
        public Lesson {
            steps = List.copyOf(steps);
        }

        public String name() {
            return "lesson";
        }
    }

    /**
     * {@code step "Contours" { object wave; view level_set; values A = 1, k = 1; narration "…" }}: one step of a lesson
     * — what is shown, how it is set up, and what is said or asked about it.
     *
     * @param object    the object shown: {@code object wave}
     * @param view      how it is shown and the representation's settings: {@code view cross_section axis Y offset 0}
     * @param values    the parameters it is set to: {@code values A = 1, k = 1}
     * @param camera    where it is looked at from: {@code camera azimuth 45 elevation 30}
     * @param narration what is said: {@code narration "…"}
     * @param task      what is asked: {@code task "question" answer "answer"}
     */
    record CourseStep(String title, Optional<String> object, Optional<Representation> view,
                      Map<String, SourceText> values, Optional<Camera> camera, Optional<String> narration,
                      Optional<Task> task) implements Block {
        public CourseStep {
            values = orderedCopy(values);
        }

        public String name() {
            return "step";
        }
    }

    /** {@code task "Where is the gradient largest?" answer "dense"}; the answer may be left out. */
    record Task(String question, Optional<String> answer) {
    }

    /** {@code objects explicit_surface, parametric_surface}: the kinds of object an instrument opens. */
    record ObjectKinds(List<String> kinds) implements Block {
        public ObjectKinds {
            kinds = List.copyOf(kinds);
        }

        public String name() {
            return "objects";
        }
    }

    /** {@code representations surface, normals}: what an instrument shows first, in order. */
    record Representations(List<String> representations) implements Block {
        public Representations {
            representations = List.copyOf(representations);
        }

        public String name() {
            return "representations";
        }
    }

    /** {@code opens wave}: the object an instrument opens with when none is chosen. */
    record Opens(String reference) implements Block {
        public String name() {
            return "opens";
        }
    }

    // ─── a graph: lines on panes, moving on one clock ────────────────────────────────────────────────

    /**
     * {@code clock t from 0 to 4 pi speed 1 loop}: the one parameter every pane moves with. The range is the
     * parameter's own when it is not written; the speed is how many passes over the range take eight seconds.
     */
    record Clock(String parameter, Optional<Range> range, double speed, boolean loop) implements Block {
        public String name() {
            return "clock";
        }
    }

    /** What a pane draws on: the plane, space, a circuit sheet played on the clock, or values as bars. */
    enum PaneKind { PLANE, SPACE, CIRCUIT, VALUES }

    /** How the panes share the screen. */
    enum PaneLayout { ROWS, COLUMNS, GRID }

    /**
     * {@code wave plane x 0 .. 4 pi y -1 .. 1}, {@code fourier space camera angled}, {@code sheet circuit uses
     * bridge-rectifier over 0 .. 20 ms}: a named view; every axis it does not fix is fitted to what it draws. A circuit
     * pane plays its circuit's transient: {@code over} is the span of circuit time the clock's range is laid onto, and
     * without it the clock's value is the time in seconds.
     */
    record Pane(String name, PaneKind kind, Map<String, Range> axes, Optional<String> uses, Optional<Range> over,
                Optional<String> camera, int line) {
        public Pane {
            axes = orderedCopy(axes);
        }
    }

    /** {@code panes rows { … }}; a graph without one draws every line on one plane. */
    record Panes(PaneLayout layout, List<Pane> panes) implements Block {
        public Panes {
            panes = List.copyOf(panes);
        }

        public String name() {
            return "panes";
        }
    }

    /**
     * {@code output y = abs(vin(x)) until t at (t, 1) in wave color 2 hidden}: an expression drawn in a pane — or, when
     * it is a text in quotes, a label whose {@code {…}} parts are read live. Every word after the expression is
     * optional and they come in this order.
     *
     * @param until  the curve is drawn only up to this value of its own variable — a trace that follows the clock
     * @param at     where a label stands
     * @param from   the circuit pane whose signal the line draws — {@code V(out) from sheet}: a probe's notation, the
     *               circuit's own computed transient, laid on the clock
     * @param pane   the pane it is drawn in; the first pane when not written
     * @param color  the series colour, 1…6
     */
    record GraphLine(String name, SourceText expression, boolean label, Optional<SourceText> until,
                     Optional<SourceText> at, Optional<String> from, Optional<String> pane, Optional<Integer> color, boolean hidden,
                     int line) {
    }

    /**
     * {@code expect "the total energy stays put" { measure p(t) + k(t);  near 1.7135 +- 0.5% }}: what a graph claims,
     * checked at every frame of its clock — a graph that shows a law says so, and the engine says whether it holds.
     *
     * @param written the criterion as written, kept so the document is written back as it was
     */
    record GraphCheck(String title, SourceText measure, Criterion criterion, String written, int line) implements Block {
        public String name() {
            return "expect";
        }
    }

    record Lines(List<GraphLine> lines) implements Block {
        public Lines {
            lines = List.copyOf(lines);
        }

        public String name() {
            return "lines";
        }
    }
}
