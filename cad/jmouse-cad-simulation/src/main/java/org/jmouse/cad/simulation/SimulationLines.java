package org.jmouse.cad.simulation;

import org.jmouse.cad.document.ArrangementLines;
import org.jmouse.el.grammar.LineGrammar;

import java.util.Arrays;
import java.util.List;

import static org.jmouse.el.grammar.Values.ASSIGNED;
import static org.jmouse.el.grammar.Values.CONNECTIONS;
import static org.jmouse.el.grammar.Values.EXTENT;
import static org.jmouse.el.grammar.Values.FLAG;
import static org.jmouse.el.grammar.Values.FORMULA;
import static org.jmouse.el.grammar.Values.ITEMS;
import static org.jmouse.el.grammar.Values.NAME;
import static org.jmouse.el.grammar.Values.NAMES;
import static org.jmouse.el.grammar.Values.NUMBERS;
import static org.jmouse.el.grammar.Values.POINT;
import static org.jmouse.el.grammar.Values.QUANTITY;
import static org.jmouse.el.grammar.Values.RANGE;
import static org.jmouse.el.grammar.Values.SETTINGS;
import static org.jmouse.el.grammar.Values.TEXT;
import static org.jmouse.el.grammar.Values.TOLERANCE;
import static org.jmouse.el.grammar.Values.WORD;

/**
 * Every line a simulated document writes, said as a grammar: a parameter, a pin, a state, a part, where a part stands, a
 * probe, an expectation's measure and what it is held to. Every expression in them is kept as written for jMF.
 */
public final class SimulationLines {

    /** {@code parameter:R(10 kΩ, in: 1 kΩ .. 100 kΩ, step: 1 kΩ, "R, the load")}, {@code parameter:operation("write", in: ["write", "read"])}. */
    public static final LineGrammar PARAMETER = LineGrammar.named("parameter", "name")
            .slot("value", ASSIGNED)
            .optional("in", RANGE)
            .optional("step", FORMULA)
            .optional("per", WORD)
            .optional("log", FLAG)
            .optional("integer", FLAG)
            .optional("exact", FLAG)
            .optional("animate", FLAG)
            .optional("live", FLAG)
            .ending("description")
            .build();

    /** {@code out analog output 1 7 "output"}, {@code q[0..7] digital output 15, 1, 2, 3, 4, 5, 6, 7}. */
    public static final LineGrammar PIN = LineGrammar.named("pin", "name")
            .slot("nature", WORD, SimulationVocabulary.PIN_NATURES)
            .optionalSlot("direction", WORD, SimulationVocabulary.PIN_DIRECTIONS)
            .optionalSlot("drive", WORD, SimulationVocabulary.PIN_DRIVES)
            .optionalSlot("numbers", NUMBERS)
            .optional("edge", FLAG)
            .ending("label")
            .build();

    /** {@code bi "the intrinsic base"}: a node of a component's own. */
    public static final LineGrammar NODE = LineGrammar.named("node", "name").ending("description")
            .build();

    /** {@code stored = X discrete in 0 .. 255 "…"}, {@code memory[256] = 0 discrete}. */
    public static final LineGrammar STATE = LineGrammar.named("state", "name")
            .slot("value", ASSIGNED)
            .slot("kind", WORD, SimulationVocabulary.STATE_KINDS)
            .optional("in", RANGE)
            .ending("description")
            .build();

    /** {@code overvoltage = 100 * … unit % "of VRRM"}: a figure written beside the part. */
    public static final LineGrammar FIGURE = LineGrammar.named("figure", "name")
            .slot("value", ASSIGNED)
            .optional("unit", WORD)
            .ending("caption")
            .build();

    /** {@code reading = plus - minus unit V signal}: what an instrument reads. */
    public static final LineGrammar READING = LineGrammar.named("reading", "name")
            .slot("value", ASSIGNED)
            .optional("unit", WORD)
            .optional("signal", FLAG)
            .build();

    /** {@code plus red stripes black}: a lead's colours. */
    public static final LineGrammar LEAD = LineGrammar.named("lead", "pin")
            .slot("colour", WORD)
            .optional("stripes", WORD)
            .build();

    /** {@code let V_be : voltage = polarity * (bi - ei)}: a name an equation gives a value, and what it is. */
    public static final LineGrammar LOCAL = local("let");

    /** {@code local} is the same word as {@code let}. */
    public static final LineGrammar LOCAL_SPELLED_LOCAL = local("local");

    /** {@code current base -> bi = (base - bi) / R_base}: a current from one pin to another, through the part. */
    public static final LineGrammar CURRENT_BRANCH = branch("current");

    /** {@code voltage out -> 0 = gain * input}: a branch holding one pin at a voltage above another. */
    public static final LineGrammar VOLTAGE_BRANCH = branch("voltage");

    private static LineGrammar local(String word) {
        return LineGrammar.line(word).slot("name", NAME).markedSlot("type", ":", WORD).slot("value", ASSIGNED).writtenAsIs()
                .build();
    }

    private static LineGrammar branch(String word) {
        return LineGrammar.line(word).slot("from", NAME).markedSlot("to", "->", NAME).slot("value", ASSIGNED).writtenAsIs()
                .check((branch, grammar) -> branch.slot("to").isEmpty() ? grammar.shape() : null)
                .build();
    }

    /** {@code failed = failed}, {@code a anode}: what a drawing's input is bound to — a value, or one of the part's pins. */
    public static final LineGrammar BINDING = LineGrammar.named("bind", "input")
            .optionalSlot("value", ASSIGNED)
            .optionalSlot("pin", WORD)
            .namedInTerms("pin")
            .check((binding, grammar) -> binding.slot("value").isPresent() == binding.slot("pin").isPresent() ? grammar.shape() : null)
            .build();

    /** {@code 1 base}: which of the part's pins a footprint's pad is — its own numbering, when the package's differs. */
    public static final LineGrammar PAD = LineGrammar.named("pad", "pad").slot("pin", WORD)
            .build();

    /** {@code left clock edge, cs bubble, data}: the pins a box draws on one of its sides, each with its marks. */
    public static final List<LineGrammar> BOX_SIDES = Arrays.stream(SimulationVocabulary.BOX_SIDES)
            .map(side -> LineGrammar.line(side).slot("pins", ITEMS).build())
            .toList();

    /** {@code analysis transient for 80 ms}: what a circuit is run as, and for how long. */
    public static final LineGrammar ANALYSIS = LineGrammar.line("analysis").slot("kind", WORD).optional("for", QUANTITY)
            .build();

    /** {@code symbol uses npn-transistor-symbol}, {@code symbol box}: how a part is drawn, the head of its block. */
    public static final LineGrammar SYMBOL = LineGrammar.line("symbol").optional("uses", NAMES).optional("box", FLAG)
            .build();

    /** {@code portrait uses to-92-portrait}: how a part looks in the hand, the head of its block. */
    public static final LineGrammar PORTRAIT = LineGrammar.line("portrait").required("uses", NAMES)
            .build();

    /** {@code footprint soic-8-footprint}: the footprint a part sits on, the head of its block. */
    public static final LineGrammar FOOTPRINT = LineGrammar.line("footprint").slot("slug", WORD)
            .build();

    /**
     * {@code R1 resistor (in, out) resistance = R symbol resistor-iec}, {@code U1 lm358@3 (A.output = out, …)},
     * {@code D1 1n4148 (anode = a, cathode = b) footprint sod-123 pads (cathode = 1, anode = 2)}.
     */
    public static final LineGrammar PART = LineGrammar.named("part", "reference")
            .slot("component", WORD)
            .optionalSlot("connections", CONNECTIONS)
            .optionalSlot("settings", SETTINGS)
            .optional("symbol", WORD)
            .optional("footprint", WORD)
            .optional("pads", CONNECTIONS)
            .build();

    /**
     * {@code R1 at 240, 200 rotate 90}, {@code U4.B at 400, 120} — one block of a part placed on its own —,
     * {@code S1 at 400, 300 size 240 x 160 ports in left 60, out right 60} — a placed sheet's box and where its ports stand
     * on it —, {@code M1 at 80, 40 shows rms graph}.
     */
    public static final LineGrammar PLACE = LineGrammar.named("place", "reference")
            .optional("at", POINT)
            .optional("rotate", QUANTITY)
            .optional("mirror", FLAG)
            .optional("size", EXTENT)
            .nested("name", LineGrammar.line("name").required("at", POINT).build())
            .nested("value", LineGrammar.line("value").required("at", POINT).build())
            .nested("readings", LineGrammar.line("readings").required("at", POINT)
                    .build())
            .optional("shows", ITEMS)
            .optional("ports", ITEMS)
            .including(ArrangementLines.CLAUSES)
            .build();

    /** {@code frame iso-a4-landscape-frame}: the frame a circuit's sheet stands in. */
    public static final LineGrammar FRAME = LineGrammar.line("frame").slot("slug", WORD)
            .build();

    /** {@code input in}, {@code output out}: a net a sheet hands to the block that places it, and which way it goes. */
    public static final List<LineGrammar> PORTS = Arrays.stream(SimulationVocabulary.PORT_DIRECTIONS)
            .map(direction -> LineGrammar.line(direction).slot("net", WORD).build())
            .toList();

    /** {@code vcc global}, {@code agnd ground}: a net named for what it is beyond the sheet. */
    public static final LineGrammar NET = LineGrammar.named("net", "name")
            .optional("ground", FLAG)
            .optional("global", FLAG)
            .build();

    /** {@code GND1 ground at 600, 520}, {@code A1 ground agnd at 640, 520}, {@code L1 label vp at 80, 40}: a mark that is no part. */
    public static final LineGrammar MARK = LineGrammar.named("mark", "reference")
            .slot("symbol", WORD, SimulationVocabulary.LAYOUT_MARKS)
            .optionalSlot("net", WORD)
            .required("at", POINT)
            .optional("rotate", QUANTITY)
            .including(ArrangementLines.CLAUSES)
            .build();

    /** {@code W1 wire R1.negative to C1.positive via 240 120}: a wire from one pin to another, through its corners. */
    public static final LineGrammar WIRE = LineGrammar.named("wire", "reference")
            .requiredMarkedSlot("from", "wire", WORD)
            .required("to", WORD)
            .optional("via", NUMBERS)
            .including(ArrangementLines.CLAUSES)
            .build();

    /** {@code T1 note "Keep this loop short" at 240, 120}: words on the sheet. */
    public static final LineGrammar NOTE = LineGrammar.named("note", "reference")
            .requiredMarkedSlot("words", "note", TEXT)
            .required("at", POINT)
            .including(ArrangementLines.CLAUSES)
            .build();

    /** {@code S1 shape rectangle via 240 120, 400 200}: a line or a shape drawn on the sheet. */
    public static final LineGrammar DRAWN = LineGrammar.named("drawn", "reference")
            .requiredMarkedSlot("shape", "shape", WORD)
            .required("via", NUMBERS)
            .including(ArrangementLines.CLAUSES)
            .build();

    /** {@code B1 bus D members D0, D1, D2 via 240 120, 400 120}: a bus by its name, the nets it carries and its corners. */
    public static final LineGrammar BUS = LineGrammar.named("bus", "reference")
            .requiredMarkedSlot("name", "bus", WORD)
            .optional("members", ITEMS)
            .required("via", NUMBERS)
            .including(ArrangementLines.CLAUSES)
            .build();

    /** {@code CH1 out 2 V/div}, {@code CH3 current R1 1 mA/div}, {@code L1 level clock}, {@code CH2 a against b 1 V/div}. */
    public static final LineGrammar PROBE = LineGrammar.named("probe", "channel")
            .optionalSlot("reading", WORD, SimulationVocabulary.PROBE_READINGS)
            .slot("subject", WORD)
            .markedSlot("reference", "against", WORD)
            .optionalSlot("scale", FORMULA)
            .optional("shows", FORMULA)
            .build();

    /** {@code time 5 ms/div}: the oscilloscope's time base. */
    public static final LineGrammar TIME_BASE = LineGrammar.line("time").slot("scale", FORMULA)
            .build();

    /** {@code transient_response primary}, {@code frequency_response from 1 to 100000 points 200}. */
    public static final LineGrammar VIEW = LineGrammar.named("view", "view")
            .optional("primary", FLAG)
            .optional("parameter", WORD)
            .optional("from", FORMULA)
            .optional("to", FORMULA)
            .optional("points", FORMULA)
            .build();

    /** {@code measure maximum out - in after 1 ms}, {@code measure final figures U1.inverted with flipped = 1}. */
    public static final LineGrammar MEASURE = LineGrammar.line("measure")
            .slot("statistic", WORD)
            .optionalSlot("subject", FORMULA)
            .optional("current", WORD)
            .optional("power", WORD)
            .optional("level", WORD)
            .optional("levels", NAMES)
            .optional("figure", WORD)
            .optional("figures", NAMES)
            .check((measure, grammar) -> SimulationVocabulary.MEASURED.stream().filter(measure::has).count()
                    + (measure.slot("subject").isPresent() ? 1 : 0) == 1 ? null
                    : "a measure measures one thing: a net or an expression, 'current R1', 'power R1', 'level clock', 'levels d0, d1', "
                    + "'figure U1.lit' or 'figures U1.a, U1.b'")
            .optional("after", QUANTITY)
            .optional("above", QUANTITY)
            .optional("below", QUANTITY)
            .optional("at", QUANTITY)
            .optional("with", SETTINGS)
            .build();

    /** {@code near 6.86 kHz +- 5%}. */
    public static final LineGrammar NEAR = LineGrammar.line("near").slot("value", FORMULA).slot("tolerance", TOLERANCE)
            .build();

    /** {@code within 26 V .. 30 V}. */
    public static final LineGrammar WITHIN = LineGrammar.line("within").slot("range", RANGE)
            .build();

    /** {@code is 1}, {@code is [1, 0, 1]}. */
    public static final LineGrammar IS = LineGrammar.line("is").slot("value", FORMULA)
            .build();

    /** {@code "the output's DC is gone" from "after 6 RC…"}: an expectation's head. */
    public static final LineGrammar EXPECTATION = LineGrammar.line("expect").slot("title", TEXT).optional("from", TEXT)
            .build();

    private SimulationLines() {
    }
}
