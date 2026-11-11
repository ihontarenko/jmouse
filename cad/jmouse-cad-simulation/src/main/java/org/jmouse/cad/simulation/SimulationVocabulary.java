package org.jmouse.cad.simulation;

import java.util.List;
import java.util.Set;

/** The words a simulated document's lines are written with, as data its grammars name. */
public final class SimulationVocabulary {

    /** {@code parts { R1 resistor … }}: the block a circuit — or a component made of parts — places them in. */
    public static final String PARTS = "parts";

    /** What a pin carries: a voltage and a current — a terminal —, a part's supply, its ground. */
    public static final String ANALOG = "analog";
    public static final String POWER = "power";
    public static final String GROUND = "ground";

    /** What a pin carries. */
    public static final String[] PIN_NATURES = {ANALOG, "digital", POWER, GROUND};

    /** Which way a pin's signal goes. */
    public static final String INPUT = "input";
    public static final String OUTPUT = "output";
    public static final String BIDIRECTIONAL = "bidirectional";
    public static final String[] PIN_DIRECTIONS = {INPUT, OUTPUT, BIDIRECTIONAL};

    /** How a pin drives. */
    public static final String[] PIN_DRIVES = {"open-drain"};

    /** How a state changes: in steps, or as a quantity the solver integrates. */
    public static final String[] STATE_KINDS = {"discrete", "continuous"};

    /** What a probe reads other than a net's voltage. */
    public static final String[] PROBE_READINGS = {"current", "level"};

    /** The marks a layout draws that are no part. */
    public static final String[] LAYOUT_MARKS = {"junction", GROUND, "label", "common", POWER, "port", "global"};

    /** Which way a sheet's port goes, as its {@code ports} block writes it. */
    public static final String[] PORT_DIRECTIONS = {INPUT, OUTPUT, POWER, GROUND, ANALOG};

    /** What a measure reads other than a net or an expression: a part's current or power, a level, a part's figures. */
    public static final List<String> MEASURED = List.of("current", "power", "level", "levels", "figure", "figures");

    /** The sides of a box a symbol draws its pins on, and the two a box of plain terminals shares them between. */
    public static final String LEFT = "left";
    public static final String RIGHT = "right";
    public static final String TOP = "top";
    public static final String BOTTOM = "bottom";
    public static final String[] BOX_SIDES = {LEFT, RIGHT, TOP, BOTTOM, "power"};

    /** The marks a box draws at a pin: a clock's edge, an inverting bubble. */
    public static final String EDGE = "edge";
    public static final String BUBBLE = "bubble";

    /**
     * What a wire names the two pins of a two-terminal part wired in pin order by — {@code R1 resistor (in, out)} is
     * reached as {@code R1.positive} and {@code R1.negative} —: its component's first pin and its second.
     */
    public static final List<String> IN_ORDER_TERMINALS = List.of("positive", "negative");

    /** A parameter a reader works while the circuit runs: {@code closed live}. */
    public static final String LIVE = "live";

    /** A parameter that takes whole numbers alone. */
    public static final String INTEGER = "integer";

    /** The words a parameter is offered with, as its grammar writes them. */
    public static final List<String> PARAMETER_FLAGS = List.of("log", INTEGER, "exact", "animate", LIVE);

    /** The type of a component held only while pressed: its live toggle springs back. */
    public static final String BUTTON = "button";

    /** The analysis a circuit runs: its transient, over a span of time. */
    public static final String TRANSIENT = "transient";

    /** The analysis — and the measurement — of a circuit at rest: every node's voltage once nothing changes. */
    public static final String OPERATING = "operating";

    /** Every analysis a circuit may be run as. */
    public static final Set<String> ANALYSES = Set.of(TRANSIENT, OPERATING);

    private SimulationVocabulary() {
    }
}
