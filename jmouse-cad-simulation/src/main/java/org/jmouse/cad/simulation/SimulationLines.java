package org.jmouse.cad.simulation;

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

    /** {@code R = 10 kΩ in 1 kΩ .. 100 kΩ step 1 kΩ "R, the load"}, {@code operation = "write" in ["write", "read"]}. */
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
            .shaped("a parameter is 'name = value [in low .. high | in [a, b]] [step s] [per unit] [log] [integer] [exact] "
                    + "[animate] [live] [\"description\"]'");

    /** {@code out analog output 1 7 "output"}, {@code q[0..7] digital output 15, 1, 2, 3, 4, 5, 6, 7}. */
    public static final LineGrammar PIN = LineGrammar.named("pin", "name")
            .slot("nature", WORD, SimulationVocabulary.PIN_NATURES)
            .optionalSlot("direction", WORD, SimulationVocabulary.PIN_DIRECTIONS)
            .optionalSlot("drive", WORD, SimulationVocabulary.PIN_DRIVES)
            .optionalSlot("numbers", NUMBERS)
            .optional("edge", FLAG)
            .ending("label")
            .shaped("a pin is 'name analog|digital|power|ground [input|output|bidirectional] [open-drain] [numbers …] [edge] "
                    + "[\"label\"]'");

    /** {@code bi "the intrinsic base"}: a node of a component's own. */
    public static final LineGrammar NODE = LineGrammar.named("node", "name").ending("description")
            .shaped("a node is 'name [\"description\"]'");

    /** {@code stored = X discrete in 0 .. 255 "…"}, {@code memory[256] = 0 discrete}. */
    public static final LineGrammar STATE = LineGrammar.named("state", "name")
            .slot("value", ASSIGNED)
            .slot("kind", WORD, SimulationVocabulary.STATE_KINDS)
            .optional("in", RANGE)
            .ending("description")
            .shaped("a state is 'name = start discrete|continuous [in low .. high] [\"description\"]'");

    /** {@code overvoltage = 100 * … unit % "of VRRM"}: a figure written beside the part. */
    public static final LineGrammar FIGURE = LineGrammar.named("figure", "name")
            .slot("value", ASSIGNED)
            .optional("unit", WORD)
            .ending("caption")
            .shaped("a figure is 'name = expression [unit U] [\"caption\"]'");

    /** {@code reading = plus - minus unit V signal}: what an instrument reads. */
    public static final LineGrammar READING = LineGrammar.named("reading", "name")
            .slot("value", ASSIGNED)
            .optional("unit", WORD)
            .optional("signal", FLAG)
            .shaped("a reading is 'name = expression [unit U] [signal]'");

    /** {@code plus red stripes black}: a lead's colours. */
    public static final LineGrammar LEAD = LineGrammar.named("lead", "pin")
            .slot("colour", WORD)
            .optional("stripes", WORD)
            .shaped("a lead is 'pin colour [stripes colour]'");

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
                .shaped("a local value is '" + word + " name [: type] = expression'");
    }

    private static LineGrammar branch(String word) {
        return LineGrammar.line(word).slot("from", NAME).markedSlot("to", "->", NAME).slot("value", ASSIGNED).writtenAsIs()
                .check((branch, grammar) -> branch.slot("to").isEmpty() ? grammar.shape() : null)
                .shaped("a branch is '" + word + " pin -> pin = expression'");
    }

    /** {@code failed = failed}, {@code a anode}: what a drawing's input is bound to — a value, or one of the part's pins. */
    public static final LineGrammar BINDING = LineGrammar.named("bind", "input")
            .optionalSlot("value", ASSIGNED)
            .optionalSlot("pin", WORD)
            .namedInTerms("pin")
            .check((binding, grammar) -> binding.slot("value").isPresent() == binding.slot("pin").isPresent() ? grammar.shape() : null)
            .shaped("a binding is 'input = expression', 'input = \"text\"' or 'input pin'");

    /** {@code 1 base}: which of the part's pins a footprint's pad is — its own numbering, when the package's differs. */
    public static final LineGrammar PAD = LineGrammar.named("pad", "pad").slot("pin", WORD)
            .shaped("a pad is 'number pin'");

    /** {@code left clock edge, cs bubble, data}: the pins a box draws on one of its sides, each with its marks. */
    public static final List<LineGrammar> BOX_SIDES = Arrays.stream(SimulationVocabulary.BOX_SIDES)
            .map(side -> LineGrammar.line(side).slot("pins", ITEMS).shaped("a side of a box is '" + side + " pin [edge] [bubble], pin …'"))
            .toList();

    /** {@code analysis transient for 80 ms}: what a circuit is run as, and for how long. */
    public static final LineGrammar ANALYSIS = LineGrammar.line("analysis").slot("kind", WORD).optional("for", QUANTITY)
            .shaped("an analysis is 'analysis transient for 80 ms'");

    /** {@code symbol uses npn-transistor-symbol}, {@code symbol box}: how a part is drawn, the head of its block. */
    public static final LineGrammar SYMBOL = LineGrammar.line("symbol").optional("uses", NAMES).optional("box", FLAG)
            .shaped("a part is drawn as 'symbol uses slug, …' or 'symbol box { side pins … }'");

    /** {@code portrait uses to-92-portrait}: how a part looks in the hand, the head of its block. */
    public static final LineGrammar PORTRAIT = LineGrammar.line("portrait").required("uses", NAMES)
            .shaped("a part looks like 'portrait uses slug'");

    /** {@code footprint soic-8-footprint}: the footprint a part sits on, the head of its block. */
    public static final LineGrammar FOOTPRINT = LineGrammar.line("footprint").slot("slug", WORD)
            .shaped("a part sits on 'footprint slug'");

    /** {@code R1 resistor (in, out) resistance = R symbol resistor-iec}, {@code U1 lm358@3 (A.output = out, …)}. */
    public static final LineGrammar PART = LineGrammar.named("part", "reference")
            .slot("component", WORD)
            .optionalSlot("connections", CONNECTIONS)
            .optionalSlot("settings", SETTINGS)
            .optional("symbol", WORD)
            .optional("footprint", WORD)
            .shaped("a part is 'reference component [(net, …) | (pin = net, …)] [setting = value, …] [symbol slug] "
                    + "[footprint slug]'");

    /**
     * {@code R1 at 240, 200 rotate 90}, {@code U4.B at 400, 120} — one block of a part placed on its own —,
     * {@code S1 at 400, 300 size 240 x 160} — a placed sheet's box —, {@code M1 at 80, 40 shows rms graph}.
     */
    public static final LineGrammar PLACE = LineGrammar.named("place", "reference")
            .optional("at", POINT)
            .optional("rotate", QUANTITY)
            .optional("mirror", FLAG)
            .optional("size", EXTENT)
            .nested("name", LineGrammar.line("name").required("at", POINT).shaped("a name stands 'name at x, y'"))
            .nested("value", LineGrammar.line("value").required("at", POINT).shaped("a value stands 'value at x, y'"))
            .nested("readings", LineGrammar.line("readings").required("at", POINT)
                    .shaped("an instrument's readings stand 'readings at x, y'"))
            .optional("shows", ITEMS)
            .shaped("a part stands 'R1 at x, y [rotate degrees] [mirror] [size w x h] [name at x, y] [value at x, y] "
                    + "[readings at x, y] [shows what …]'");

    /** {@code frame iso-a4-landscape-frame}: the frame a circuit's sheet stands in. */
    public static final LineGrammar FRAME = LineGrammar.line("frame").slot("slug", WORD)
            .shaped("a frame line is 'frame <slug>': the frame the sheet stands in");

    /** {@code input in}, {@code output out}: a net a sheet hands to the block that places it, and which way it goes. */
    public static final List<LineGrammar> PORTS = Arrays.stream(SimulationVocabulary.PORT_DIRECTIONS)
            .map(direction -> LineGrammar.line(direction).slot("net", WORD).shaped("a port is '" + direction + " net'"))
            .toList();

    /** {@code vcc global}, {@code agnd ground}: a net named for what it is beyond the sheet. */
    public static final LineGrammar NET = LineGrammar.named("net", "name")
            .optional("ground", FLAG)
            .optional("global", FLAG)
            .shaped("a net line is 'name [ground] [global]'");

    /** {@code GND1 ground at 600, 520}, {@code A1 ground agnd at 640, 520}, {@code L1 label vp at 80, 40}: a mark that is no part. */
    public static final LineGrammar MARK = LineGrammar.named("mark", "reference")
            .slot("symbol", WORD, SimulationVocabulary.LAYOUT_MARKS)
            .optionalSlot("net", WORD)
            .required("at", POINT)
            .optional("rotate", QUANTITY)
            .shaped("a mark stands 'GND1 ground [net] at x, y [rotate degrees]' or 'L1 label net at x, y'");

    /** {@code W1 wire R1.negative to C1.positive via 240 120}: a wire from one pin to another, through its corners. */
    public static final LineGrammar WIRE = LineGrammar.named("wire", "reference")
            .requiredMarkedSlot("from", "wire", WORD)
            .required("to", WORD)
            .optional("via", NUMBERS)
            .shaped("a wire is 'W1 wire R1.a to C1.b [via x y, x y …]'");

    /** {@code CH1 out 2 V/div}, {@code CH3 current R1 1 mA/div}, {@code L1 level clock}, {@code CH2 a against b 1 V/div}. */
    public static final LineGrammar PROBE = LineGrammar.named("probe", "channel")
            .optionalSlot("reading", WORD, SimulationVocabulary.PROBE_READINGS)
            .slot("subject", WORD)
            .markedSlot("reference", "against", WORD)
            .optionalSlot("scale", FORMULA)
            .optional("shows", FORMULA)
            .shaped("a probe is 'CH1 net [against net] scale/div [shows expression]', 'CH2 current R1 scale/div' or 'L1 level net'");

    /** {@code time 5 ms/div}: the oscilloscope's time base. */
    public static final LineGrammar TIME_BASE = LineGrammar.line("time").slot("scale", FORMULA)
            .shaped("the time base is 'time 5 ms/div'");

    /** {@code transient_response primary}, {@code frequency_response from 1 to 100000 points 200}. */
    public static final LineGrammar VIEW = LineGrammar.named("view", "view")
            .optional("primary", FLAG)
            .optional("parameter", WORD)
            .optional("from", FORMULA)
            .optional("to", FORMULA)
            .optional("points", FORMULA)
            .shaped("a view is 'name [primary] [parameter P] [from a to b points n]'");

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
            .shaped("a measure is 'measure statistic subject|current part|level net|levels nets|figure name|figures names "
                    + "[above level] [below level] [at frequency] [after time] "
                    + "[with setting = value, …]'");

    /** {@code near 6.86 kHz +- 5%}. */
    public static final LineGrammar NEAR = LineGrammar.line("near").slot("value", FORMULA).slot("tolerance", TOLERANCE)
            .shaped("an expectation is 'near value ± tolerance'");

    /** {@code within 26 V .. 30 V}. */
    public static final LineGrammar WITHIN = LineGrammar.line("within").slot("range", RANGE)
            .shaped("an expectation is 'within low .. high'");

    /** {@code is 1}, {@code is [1, 0, 1]}. */
    public static final LineGrammar IS = LineGrammar.line("is").slot("value", FORMULA)
            .shaped("an expectation is 'is value'");

    /** {@code "the output's DC is gone" from "after 6 RC…"}: an expectation's head. */
    public static final LineGrammar EXPECTATION = LineGrammar.line("expect").slot("title", TEXT).optional("from", TEXT)
            .shaped("an expectation is 'expect \"what holds\" [from \"why\"] { … }'");

    private SimulationLines() {
    }
}
