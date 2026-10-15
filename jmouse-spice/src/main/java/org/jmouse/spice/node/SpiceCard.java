package org.jmouse.spice.node;

import org.jmouse.el.node.Trivia;
import org.jmouse.spice.SpicePosition;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** A dot card: {@code .model}, {@code .subckt}, {@code .param}, an analysis, {@code .end} and the rest. */
public abstract sealed class SpiceCard extends SpiceStatement {

    protected SpiceCard(SpicePosition position) {
        super(position);
    }

    /** {@code .model name type(name=value …)}. */
    public static final class Model extends SpiceCard {

        private final String               name;
        private final String               type;
        private final List<SpiceParameter> parameters;

        /** @param type the device type as written: {@code D}, {@code NPN}, {@code NMOS}, {@code SW} … */
        public Model(String name, String type, List<SpiceParameter> parameters, SpicePosition position) {
            super(position);
            this.name = name;
            this.type = type;
            this.parameters = List.copyOf(parameters);
        }

        public String name() {
            return name;
        }

        public String type() {
            return type;
        }

        public List<SpiceParameter> parameters() {
            return parameters;
        }

        @Override
        public <R> R accept(SpiceVisitor<R> visitor) {
            return visitor.visitModel(this);
        }
    }

    /** {@code .subckt name pins… [params: name=value …]} … {@code .ends}: its statements are its children. */
    public static final class Subcircuit extends SpiceCard {

        private final String               name;
        private final List<String>         pins;
        private final List<SpiceParameter> parameters;
        private final List<Trivia>         closingTrivia = new ArrayList<>();

        public Subcircuit(String name, List<String> pins, List<SpiceParameter> parameters, SpicePosition position) {
            super(position);
            this.name = name;
            this.pins = List.copyOf(pins);
            this.parameters = List.copyOf(parameters);
        }

        /** A subcircuit a program builds, with its statements. */
        public static Subcircuit of(String name, List<String> pins, List<SpiceParameter> parameters,
                                    List<? extends SpiceStatement> statements) {
            Subcircuit subcircuit = new Subcircuit(name, pins, parameters, SpicePosition.NONE);
            statements.forEach(subcircuit::add);
            return subcircuit;
        }

        public String name() {
            return name;
        }

        public List<String> pins() {
            return pins;
        }

        public List<SpiceParameter> parameters() {
            return parameters;
        }

        public List<SpiceStatement> statements() {
            return getChildren(SpiceStatement.class);
        }

        /** The comments between its last statement and its {@code .ends}. */
        public List<Trivia> closingTrivia() {
            return List.copyOf(closingTrivia);
        }

        public void addClosingTrivia(List<Trivia> trivia) {
            closingTrivia.addAll(trivia);
        }

        @Override
        public <R> R accept(SpiceVisitor<R> visitor) {
            return visitor.visitSubcircuit(this);
        }
    }

    /** {@code .param R=1k C={1/(2*pi*R*f)}}. */
    public static final class Parameters extends SpiceCard {

        private final List<SpiceParameter> parameters;

        public Parameters(List<SpiceParameter> parameters, SpicePosition position) {
            super(position);
            this.parameters = List.copyOf(parameters);
        }

        public List<SpiceParameter> parameters() {
            return parameters;
        }

        @Override
        public <R> R accept(SpiceVisitor<R> visitor) {
            return visitor.visitParameters(this);
        }
    }

    /** {@code .include "file"}, {@code .lib "file" section}: a file the deck names. Reading it is the caller's. */
    public static final class Include extends SpiceCard {

        private final String directive;
        private final String path;
        private final String section;

        /**
         * @param directive {@code include}, {@code inc} or {@code lib}, as written in lower case
         * @param path      the file as written, without its quotes
         * @param section   a library's section, or {@code null}
         */
        public Include(String directive, String path, String section, SpicePosition position) {
            super(position);
            this.directive = directive;
            this.path = path;
            this.section = section;
        }

        public String directive() {
            return directive;
        }

        public String path() {
            return path;
        }

        public Optional<String> section() {
            return Optional.ofNullable(section);
        }

        @Override
        public <R> R accept(SpiceVisitor<R> visitor) {
            return visitor.visitInclude(this);
        }
    }

    /** {@code .tran step stop [start [largest step]] [uic]}. */
    public static final class Transient extends SpiceCard {

        private final SpiceValue step;
        private final SpiceValue stop;
        private final SpiceValue start;
        private final SpiceValue largestStep;
        private final boolean    initialConditions;

        /** @param start and {@code largestStep} are {@code null} when not written */
        public Transient(SpiceValue step, SpiceValue stop, SpiceValue start, SpiceValue largestStep,
                         boolean initialConditions, SpicePosition position) {
            super(position);
            this.step = step;
            this.stop = stop;
            this.start = start;
            this.largestStep = largestStep;
            this.initialConditions = initialConditions;
        }

        public SpiceValue step() {
            return step;
        }

        public SpiceValue stop() {
            return stop;
        }

        public Optional<SpiceValue> start() {
            return Optional.ofNullable(start);
        }

        public Optional<SpiceValue> largestStep() {
            return Optional.ofNullable(largestStep);
        }

        /** Whether it is written with {@code uic}: start from the initial conditions, not an operating point. */
        public boolean initialConditions() {
            return initialConditions;
        }

        @Override
        public <R> R accept(SpiceVisitor<R> visitor) {
            return visitor.visitTransient(this);
        }
    }

    /** {@code .ac dec|oct|lin points start stop}. */
    public static final class Ac extends SpiceCard {

        private final String     variation;
        private final SpiceValue points;
        private final SpiceValue start;
        private final SpiceValue stop;

        public Ac(String variation, SpiceValue points, SpiceValue start, SpiceValue stop, SpicePosition position) {
            super(position);
            this.variation = variation;
            this.points = points;
            this.start = start;
            this.stop = stop;
        }

        public String variation() {
            return variation;
        }

        public SpiceValue points() {
            return points;
        }

        public SpiceValue start() {
            return start;
        }

        public SpiceValue stop() {
            return stop;
        }

        @Override
        public <R> R accept(SpiceVisitor<R> visitor) {
            return visitor.visitAc(this);
        }
    }

    /** {@code .dc source start stop step [source2 start2 stop2 step2]}: its sweeps as written. */
    public static final class Dc extends SpiceCard {

        private final List<SpiceArgument> sweeps;

        public Dc(List<SpiceArgument> sweeps, SpicePosition position) {
            super(position);
            this.sweeps = List.copyOf(sweeps);
        }

        public List<SpiceArgument> sweeps() {
            return sweeps;
        }

        @Override
        public <R> R accept(SpiceVisitor<R> visitor) {
            return visitor.visitDc(this);
        }
    }

    /** {@code .op}. */
    public static final class OperatingPoint extends SpiceCard {

        public OperatingPoint(SpicePosition position) {
            super(position);
        }

        @Override
        public <R> R accept(SpiceVisitor<R> visitor) {
            return visitor.visitOperatingPoint(this);
        }
    }

    /** {@code .control} … {@code .endc}: a script for SPICE's interpreter, its lines as written. */
    public static final class Control extends SpiceCard {

        private final List<String> lines;

        public Control(List<String> lines, SpicePosition position) {
            super(position);
            this.lines = List.copyOf(lines);
        }

        public List<String> lines() {
            return lines;
        }

        @Override
        public <R> R accept(SpiceVisitor<R> visitor) {
            return visitor.visitControl(this);
        }
    }

    /** Any other dot card — {@code .options}, {@code .print}, {@code .temp}, {@code .ic} —: its word and arguments. */
    public static final class Directive extends SpiceCard {

        private final String              directive;
        private final List<SpiceArgument> arguments;

        /** @param directive its word in lower case, without the dot */
        public Directive(String directive, List<SpiceArgument> arguments, SpicePosition position) {
            super(position);
            this.directive = directive;
            this.arguments = List.copyOf(arguments);
        }

        public String directive() {
            return directive;
        }

        public List<SpiceArgument> arguments() {
            return arguments;
        }

        @Override
        public <R> R accept(SpiceVisitor<R> visitor) {
            return visitor.visitDirective(this);
        }
    }

    /** {@code .end}. */
    public static final class End extends SpiceCard {

        public End(SpicePosition position) {
            super(position);
        }

        @Override
        public <R> R accept(SpiceVisitor<R> visitor) {
            return visitor.visitEnd(this);
        }
    }
}
