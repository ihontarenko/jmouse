package org.jmouse.spice.node;

import org.jmouse.spice.SpicePosition;

import java.util.List;
import java.util.Optional;

/**
 * An element line: its name first, the name's letter saying what it is. Nodes are written as SPICE writes them, in
 * the case they were written; SPICE compares them without regard to case.
 */
public abstract sealed class SpiceElement extends SpiceStatement {

    private final String name;

    protected SpiceElement(String name, SpicePosition position) {
        super(position);
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("An element has a name.");
        }
        this.name = name;
    }

    public String name() {
        return name;
    }

    /** The element's letter, upper case: {@code R}, {@code V}, {@code X}. */
    public char letter() {
        return Character.toUpperCase(name.charAt(0));
    }

    // ─── R C L ──────────────────────────────────────────────────────────────────────────────────────

    /** {@code R1 in out 1k}, {@code C1 out 0 {C} IC=0}, {@code L1 a b 10m}. */
    public static final class Passive extends SpiceElement {

        /** What a passive element is, by its letter. */
        public enum Kind { RESISTOR, CAPACITOR, INDUCTOR }

        private final String              positive;
        private final String              negative;
        private final SpiceValue          value;
        private final List<SpiceArgument> rest;

        public Passive(String name, String positive, String negative, SpiceValue value, List<SpiceArgument> rest,
                       SpicePosition position) {
            super(name, position);
            this.positive = positive;
            this.negative = negative;
            this.value = value;
            this.rest = List.copyOf(rest);
        }

        public Kind kind() {
            return switch (letter()) {
                case 'R' -> Kind.RESISTOR;
                case 'C' -> Kind.CAPACITOR;
                default -> Kind.INDUCTOR;
            };
        }

        public String positive() {
            return positive;
        }

        public String negative() {
            return negative;
        }

        public SpiceValue value() {
            return value;
        }

        /** What follows the value: a model, temperature coefficients, an initial condition. */
        public List<SpiceArgument> rest() {
            return rest;
        }

        @Override
        public <R> R accept(SpiceVisitor<R> visitor) {
            return visitor.visitPassive(this);
        }
    }

    // ─── V I ────────────────────────────────────────────────────────────────────────────────────────

    /** {@code V1 in 0 DC 5}, {@code V2 a b SIN(0 1 50)}, {@code I1 0 x 1m AC 1}. */
    public static final class Source extends SpiceElement {

        private final String                 positive;
        private final String                 negative;
        private final SpiceValue             direct;
        private final boolean                directWord;
        private final SpiceWaveform          waveform;
        private final List<SpiceArgument>    rest;

        /**
         * @param direct     its DC value, or {@code null} when none is written
         * @param directWord whether the value was written after the word {@code DC}
         * @param waveform   its transient function, or {@code null}
         * @param rest       what else it says: an {@code AC} specification, a distortion input
         */
        public Source(String name, String positive, String negative, SpiceValue direct, boolean directWord,
                      SpiceWaveform waveform, List<SpiceArgument> rest, SpicePosition position) {
            super(name, position);
            this.positive = positive;
            this.negative = negative;
            this.direct = direct;
            this.directWord = directWord;
            this.waveform = waveform;
            this.rest = List.copyOf(rest);
        }

        /** Whether it is a voltage source; a current source otherwise. */
        public boolean isVoltage() {
            return letter() == 'V';
        }

        public String positive() {
            return positive;
        }

        public String negative() {
            return negative;
        }

        public Optional<SpiceValue> direct() {
            return Optional.ofNullable(direct);
        }

        public boolean directWord() {
            return directWord;
        }

        public Optional<SpiceWaveform> waveform() {
            return Optional.ofNullable(waveform);
        }

        public List<SpiceArgument> rest() {
            return rest;
        }

        @Override
        public <R> R accept(SpiceVisitor<R> visitor) {
            return visitor.visitSource(this);
        }
    }

    // ─── semiconductors ─────────────────────────────────────────────────────────────────────────────

    /** {@code D1 anode cathode model [area] [OFF]}. */
    public static final class Diode extends SpiceElement {

        private final String              anode;
        private final String              cathode;
        private final String              model;
        private final List<SpiceArgument> rest;

        public Diode(String name, String anode, String cathode, String model, List<SpiceArgument> rest,
                     SpicePosition position) {
            super(name, position);
            this.anode = anode;
            this.cathode = cathode;
            this.model = model;
            this.rest = List.copyOf(rest);
        }

        public String anode() {
            return anode;
        }

        public String cathode() {
            return cathode;
        }

        public String model() {
            return model;
        }

        public List<SpiceArgument> rest() {
            return rest;
        }

        @Override
        public <R> R accept(SpiceVisitor<R> visitor) {
            return visitor.visitDiode(this);
        }
    }

    /** {@code Q1 collector base emitter [substrate] model [area]}. */
    public static final class Bipolar extends SpiceElement {

        private final String              collector;
        private final String              base;
        private final String              emitter;
        private final String              substrate;
        private final String              model;
        private final List<SpiceArgument> rest;

        /** @param substrate its fourth node, or {@code null} when it has none */
        public Bipolar(String name, String collector, String base, String emitter, String substrate, String model,
                       List<SpiceArgument> rest, SpicePosition position) {
            super(name, position);
            this.collector = collector;
            this.base = base;
            this.emitter = emitter;
            this.substrate = substrate;
            this.model = model;
            this.rest = List.copyOf(rest);
        }

        public String collector() {
            return collector;
        }

        public String base() {
            return base;
        }

        public String emitter() {
            return emitter;
        }

        public Optional<String> substrate() {
            return Optional.ofNullable(substrate);
        }

        public String model() {
            return model;
        }

        public List<SpiceArgument> rest() {
            return rest;
        }

        @Override
        public <R> R accept(SpiceVisitor<R> visitor) {
            return visitor.visitBipolar(this);
        }
    }

    /** {@code M1 drain gate source bulk model [L=… W=…]}. */
    public static final class Mosfet extends SpiceElement {

        private final String              drain;
        private final String              gate;
        private final String              source;
        private final String              bulk;
        private final String              model;
        private final List<SpiceArgument> rest;

        public Mosfet(String name, String drain, String gate, String source, String bulk, String model,
                      List<SpiceArgument> rest, SpicePosition position) {
            super(name, position);
            this.drain = drain;
            this.gate = gate;
            this.source = source;
            this.bulk = bulk;
            this.model = model;
            this.rest = List.copyOf(rest);
        }

        public String drain() {
            return drain;
        }

        public String gate() {
            return gate;
        }

        public String source() {
            return source;
        }

        public String bulk() {
            return bulk;
        }

        public String model() {
            return model;
        }

        /** Its geometry and the rest: {@code L=1u}, {@code W=10u}. */
        public List<SpiceArgument> rest() {
            return rest;
        }

        @Override
        public <R> R accept(SpiceVisitor<R> visitor) {
            return visitor.visitMosfet(this);
        }
    }

    // ─── X ──────────────────────────────────────────────────────────────────────────────────────────

    /** {@code X1 nodes… subcircuit [params:] [name=value …]}. */
    public static final class Instance extends SpiceElement {

        private final List<String>         nodes;
        private final String               subcircuit;
        private final List<SpiceParameter> parameters;

        public Instance(String name, List<String> nodes, String subcircuit, List<SpiceParameter> parameters,
                        SpicePosition position) {
            super(name, position);
            this.nodes = List.copyOf(nodes);
            this.subcircuit = subcircuit;
            this.parameters = List.copyOf(parameters);
        }

        public List<String> nodes() {
            return nodes;
        }

        public String subcircuit() {
            return subcircuit;
        }

        public List<SpiceParameter> parameters() {
            return parameters;
        }

        @Override
        public <R> R accept(SpiceVisitor<R> visitor) {
            return visitor.visitInstance(this);
        }
    }

    // ─── B E F G H K ────────────────────────────────────────────────────────────────────────────────

    /** {@code B1 n+ n- V=expression} or {@code I=expression}: a behavioural source. */
    public static final class Behavioural extends SpiceElement {

        private final String  positive;
        private final String  negative;
        private final boolean voltage;
        private final String  expression;

        /** @param expression the expression after {@code V=} or {@code I=}, as written */
        public Behavioural(String name, String positive, String negative, boolean voltage, String expression,
                           SpicePosition position) {
            super(name, position);
            this.positive = positive;
            this.negative = negative;
            this.voltage = voltage;
            this.expression = expression;
        }

        public String positive() {
            return positive;
        }

        public String negative() {
            return negative;
        }

        /** Whether it sets a voltage ({@code V=}); a current ({@code I=}) otherwise. */
        public boolean isVoltage() {
            return voltage;
        }

        public String expression() {
            return expression;
        }

        @Override
        public <R> R accept(SpiceVisitor<R> visitor) {
            return visitor.visitBehavioural(this);
        }
    }

    /**
     * {@code E1 out+ out- in+ in- gain}, {@code G1 …} — controlled by a voltage —, {@code F1 out+ out- Vsense gain},
     * {@code H1 …} — by the current through a voltage source; the control kept as written, {@code POLY} forms included.
     */
    public static final class Controlled extends SpiceElement {

        private final String              positive;
        private final String              negative;
        private final List<SpiceArgument> control;

        public Controlled(String name, String positive, String negative, List<SpiceArgument> control,
                          SpicePosition position) {
            super(name, position);
            this.positive = positive;
            this.negative = negative;
            this.control = List.copyOf(control);
        }

        public String positive() {
            return positive;
        }

        public String negative() {
            return negative;
        }

        public List<SpiceArgument> control() {
            return control;
        }

        /** Whether a voltage controls it ({@code E}, {@code G}); a current otherwise ({@code F}, {@code H}). */
        public boolean isVoltageControlled() {
            return letter() == 'E' || letter() == 'G';
        }

        /** Whether it is a voltage source ({@code E}, {@code H}); a current source otherwise ({@code G}, {@code F}). */
        public boolean isVoltageSource() {
            return letter() == 'E' || letter() == 'H';
        }

        @Override
        public <R> R accept(SpiceVisitor<R> visitor) {
            return visitor.visitControlled(this);
        }
    }

    /** {@code K1 L1 L2 0.99}: inductors coupled by a coefficient. */
    public static final class Coupling extends SpiceElement {

        private final List<String> inductors;
        private final SpiceValue   coefficient;

        public Coupling(String name, List<String> inductors, SpiceValue coefficient, SpicePosition position) {
            super(name, position);
            this.inductors = List.copyOf(inductors);
            this.coefficient = coefficient;
        }

        public List<String> inductors() {
            return inductors;
        }

        public SpiceValue coefficient() {
            return coefficient;
        }

        @Override
        public <R> R accept(SpiceVisitor<R> visitor) {
            return visitor.visitCoupling(this);
        }
    }

    /** Any other element — a switch, a line, a JFET, a code model —: its arguments as written. */
    public static final class Generic extends SpiceElement {

        private final List<SpiceArgument> arguments;

        public Generic(String name, List<SpiceArgument> arguments, SpicePosition position) {
            super(name, position);
            this.arguments = List.copyOf(arguments);
        }

        public List<SpiceArgument> arguments() {
            return arguments;
        }

        @Override
        public <R> R accept(SpiceVisitor<R> visitor) {
            return visitor.visitGenericElement(this);
        }
    }
}
