package org.jmouse.cad.simulation;

import org.jmouse.el.grammar.ClauseBlockParser;
import org.jmouse.el.grammar.ClauseBlockParser.Body;
import org.jmouse.el.grammar.ClauseLineParser;
import org.jmouse.el.grammar.BlockParser;
import org.jmouse.cad.document.DocumentKind;
import org.jmouse.cad.document.DocumentModule;
import org.jmouse.el.grammar.FormulaLineParser;
import org.jmouse.el.grammar.LineGrammar;
import org.jmouse.el.grammar.LineReader;
import org.jmouse.el.grammar.LineSet;
import org.jmouse.cad.document.ScriptBlockParser;

import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.type.ValueType;
import org.jmouse.script.el.lexer.ScriptToken;

import java.util.ArrayList;
import java.util.List;

import static org.jmouse.el.grammar.Values.ASSIGNED;
import static org.jmouse.el.grammar.Values.FLAG;
import static org.jmouse.el.grammar.Values.NAME;
import static org.jmouse.el.grammar.Values.NAMES;
import static org.jmouse.el.grammar.Values.QUANTITY;
import static org.jmouse.el.grammar.Values.WORD;

/**
 * The simulated documents: a {@code component} — a part's parameters, pins, equations for jMF and behaviour for jMS, how
 * it is drawn and what it is expected to do —, a {@code circuit} — parts, where they stand, probes, what the run must
 * show —, a {@code family} and a library of {@code functions} its members share, a {@code category} of the catalogue.
 * Every block is said as data ({@link ClauseBlockParser}); none is a parser class of its own.
 */
public final class SimulationModule implements DocumentModule {

    private static final String FORMULA_SHAPE = "a line here is 'left = right': a definition, an equation, a law";

    private static final List<QuantityType> QUANTITIES = QuantityTypes.load();

    private static final LineSet PARTS = LineSet.of(List.of(SimulationLines.PART));

    /** {@code bench i2c-bus for 1 ms { parts … }}: the parts a component is tried among. */
    private static final ClauseBlockParser BENCH = new ClauseBlockParser("bench",
            LineGrammar.line("bench").optionalSlot("name", WORD).optional("for", QUANTITY).optional("at", QUANTITY)
                    .optional("realistic", FLAG)
                    .shaped("a bench is 'bench [name] [for time] [at temperature] [realistic] { parts … }'"), Body.REQUIRED, PARTS);

    private static final List<BlockParser<?>> BLOCKS = List.of(
            ClauseBlockParser.of("parameters", LineSet.of(List.of(SimulationLines.PARAMETER))),
            ClauseBlockParser.of("pins", LineSet.of(List.of(SimulationLines.PIN))),
            ClauseBlockParser.of("nodes", LineSet.of(List.of(SimulationLines.NODE))),
            ClauseBlockParser.of("state", LineSet.of(List.of(SimulationLines.STATE))),
            ClauseBlockParser.of("display", LineSet.of(List.of(SimulationLines.FIGURE))),
            ClauseBlockParser.of("measure", LineSet.of(List.of(SimulationLines.READING))),
            ClauseBlockParser.of("leads", LineSet.of(List.of(SimulationLines.LEAD))),
            ClauseBlockParser.of("equations", equationLines()),
            ClauseBlockParser.of("formulas", LineSet.of(new FormulaLineParser(FORMULA_SHAPE))),
            ClauseBlockParser.of("heat", equationLines()),
            new ScriptBlockParser("behaviour", ScriptToken.T_BEHAVIOUR),
            new ScriptBlockParser("run", BasicToken.T_IDENTIFIER),
            new ClauseBlockParser("symbol", SimulationLines.SYMBOL, Body.OPTIONAL, symbolLines()),
            new ClauseBlockParser(SimulationLines.PORTRAIT.word(), SimulationLines.PORTRAIT, Body.OPTIONAL,
                    LineSet.of(List.of(SimulationLines.BINDING))),
            new ClauseBlockParser("footprint", SimulationLines.FOOTPRINT, Body.OPTIONAL,
                    LineSet.of(List.of(SimulationLines.PAD))),
            ClauseBlockParser.headed("type",
                    LineGrammar.line("type").slot("name", NAME).slot("value", ASSIGNED)
                            .shaped("a type is 'type name = quantity / quantity ^ n'")),
            ClauseBlockParser.headed("analysis", SimulationLines.ANALYSIS),
            BENCH,
            new ClauseBlockParser("expect", SimulationLines.EXPECTATION, Body.REQUIRED,
                    LineSet.of(BENCH, line(SimulationLines.MEASURE), line(SimulationLines.NEAR),
                            line(SimulationLines.WITHIN), line(SimulationLines.IS))),
            ClauseBlockParser.of(SimulationVocabulary.PARTS, PARTS),
            ClauseBlockParser.of("layout", LineSet.of(List.of(SimulationLines.FRAME, SimulationLines.WIRE, SimulationLines.MARK,
                    SimulationLines.PLACE))),
            ClauseBlockParser.of("ports", LineSet.of(SimulationLines.PORTS)),
            ClauseBlockParser.of("nets", LineSet.of(List.of(SimulationLines.NET))),
            ClauseBlockParser.of("probes", LineSet.of(List.of(SimulationLines.TIME_BASE, SimulationLines.PROBE))),
            ClauseBlockParser.of("view", LineSet.of(List.of(SimulationLines.VIEW))));

    public static final DocumentKind COMPONENT = DocumentKind.of("component", "parameters", "pins", "nodes", "state",
                    "display", "measure", "leads", "equations", "formulas", "behaviour", "symbol", "portrait", "footprint",
                    "type", "bench", "expect", "parts")
            .repeating("type", "expect", "footprint");
    public static final DocumentKind CIRCUIT   = DocumentKind.of("circuit", "parameters", "ports", "nets", "parts", "layout", "probes",
                    "analysis", "view", "expect", "run", "behaviour")
            .repeating("expect");
    public static final DocumentKind FAMILY    = DocumentKind.of("family", "parameters", "formulas");
    public static final DocumentKind FUNCTIONS = DocumentKind.of("functions", "formulas", "heat");
    public static final DocumentKind CATEGORY  = DocumentKind.of("category");

    @Override
    public List<DocumentKind> kinds() {
        return List.of(COMPONENT, CIRCUIT, FAMILY, FUNCTIONS, CATEGORY);
    }

    @Override
    public List<BlockParser<?>> blocks() {
        return BLOCKS;
    }

    /** The quantities a value may be typed as, in a {@code let} of jMS or of an equation. */
    @Override
    public List<ValueType> getTypes() {
        return List.copyOf(QUANTITIES);
    }

    /** A symbol's body: the sides of its box, or what its drawing's inputs are bound to. */
    private static LineSet symbolLines() {
        List<LineGrammar> lines = new ArrayList<>(SimulationLines.BOX_SIDES);
        lines.add(SimulationLines.BINDING);
        return LineSet.of(lines);
    }

    /**
     * What a body of equations holds: a local value and a branch, each by its own form, and any other line
     * {@code left = right} — a derivative {@code d(x)/dt = …} among them — for jMF, which says what its left side is.
     */
    private static LineSet equationLines() {
        return LineSet.of(line(SimulationLines.LOCAL), line(SimulationLines.LOCAL_SPELLED_LOCAL),
                line(SimulationLines.CURRENT_BRANCH), line(SimulationLines.VOLTAGE_BRANCH), new FormulaLineParser(FORMULA_SHAPE));
    }

    private static LineReader line(LineGrammar grammar) {
        return new ClauseLineParser(grammar);
    }
}
