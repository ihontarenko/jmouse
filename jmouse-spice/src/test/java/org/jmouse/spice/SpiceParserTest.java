package org.jmouse.spice;

import org.jmouse.el.node.Trivia;
import org.jmouse.spice.node.SpiceCard;
import org.jmouse.spice.node.SpiceDeck;
import org.jmouse.spice.node.SpiceElement;
import org.jmouse.spice.node.SpiceStatement;
import org.jmouse.spice.node.SpiceValue;
import org.jmouse.spice.node.SpiceWaveform;
import org.jmouse.spice.parser.SpiceParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpiceParserTest {

    private final SpiceParser parser = new SpiceParser();

    @Test
    void readsTheTitleTheCardsAndTheirValuesAsWritten() {
        SpiceDeck deck = parser.parse("""
                RC low-pass
                * the source
                V1 in 0 SIN(0 1 1k) ; a kilohertz
                R1 in out 1k
                C1 out 0 {Cval}
                .param Cval=100n
                .tran 1u 5m
                .end
                """);
        assertEquals("RC low-pass", deck.title().orElseThrow());
        List<SpiceStatement> statements = deck.statements();
        assertEquals(6, statements.size());

        SpiceElement.Source source = assertInstanceOf(SpiceElement.Source.class, statements.get(0));
        assertTrue(source.isVoltage());
        SpiceWaveform sine = source.waveform().orElseThrow();
        assertEquals(SpiceWaveform.Kind.SIN, sine.kind());
        assertEquals(1000, ((SpiceValue.Number) sine.values().get(2)).value());
        assertEquals(List.of(Trivia.comment("* the source")), source.getLeadingTrivia());
        assertEquals("; a kilohertz", source.getTrailingTrivia().text());

        SpiceElement.Passive resistor = assertInstanceOf(SpiceElement.Passive.class, statements.get(1));
        assertEquals(SpiceElement.Passive.Kind.RESISTOR, resistor.kind());
        assertEquals("1k", resistor.value().written());
        assertEquals(new SpicePosition(4, 1), resistor.position());

        SpiceElement.Passive capacitor = assertInstanceOf(SpiceElement.Passive.class, statements.get(2));
        assertEquals("Cval", ((SpiceValue.Expression) capacitor.value()).expression().text());

        SpiceCard.Transient analysis = assertInstanceOf(SpiceCard.Transient.class, statements.get(4));
        assertEquals(5e-3, ((SpiceValue.Number) analysis.stop()).value(), 1e-18);
        assertInstanceOf(SpiceCard.End.class, statements.get(5));
    }

    @Test
    void joinsContinuationLinesAndReadsSubcircuitsWithParameters() {
        SpiceDeck deck = parser.parse("""
                .subckt divider top middle bottom params: ratio=0.5
                + total=10k
                R1 top middle {total*(1-ratio)}
                * between the halves
                R2 middle
                + bottom {total*ratio}
                .ends divider
                """);
        assertTrue(deck.title().isEmpty());
        SpiceCard.Subcircuit subcircuit = assertInstanceOf(SpiceCard.Subcircuit.class, deck.statements().getFirst());
        assertEquals(List.of("top", "middle", "bottom"), subcircuit.pins());
        assertEquals(2, subcircuit.parameters().size());
        assertEquals("total", subcircuit.parameters().get(1).name());
        assertEquals(2, subcircuit.statements().size());
        SpiceElement.Passive lower = (SpiceElement.Passive) subcircuit.statements().get(1);
        assertEquals("bottom", lower.negative());
        assertEquals(List.of(Trivia.comment("* between the halves")), lower.getLeadingTrivia());
    }

    @Test
    void readsSemiconductorsAndTellsASubstrateFromAModel() {
        SpiceDeck deck = parser.parse("""
                transistors
                Q1 c b e QX
                Q2 c b e sub QX 2
                M1 d g s s NM L=1u W=10u
                D1 a k DX
                .model QX NPN(IS=1e-14 BF=200)
                .model NM NMOS VTO=1 KP=2e-5
                .model DX D(IS=2.52n RS=0.568)
                """);
        SpiceElement.Bipolar first = (SpiceElement.Bipolar) deck.statements().get(0);
        assertTrue(first.substrate().isEmpty());
        assertEquals("QX", first.model());
        SpiceElement.Bipolar second = (SpiceElement.Bipolar) deck.statements().get(1);
        assertEquals("sub", second.substrate().orElseThrow());
        assertEquals("2", second.rest().getFirst().written());
        SpiceElement.Mosfet mosfet = (SpiceElement.Mosfet) deck.statements().get(2);
        assertEquals("NM", mosfet.model());
        assertEquals(2, mosfet.rest().size());
        SpiceCard.Model model = (SpiceCard.Model) deck.statements().get(4);
        assertEquals("NPN", model.type());
        assertEquals(200, ((SpiceValue.Number) model.parameters().get(1).value()).value());
        SpiceCard.Model withoutParentheses = (SpiceCard.Model) deck.statements().get(5);
        assertEquals(2, withoutParentheses.parameters().size());
    }

    @Test
    void keepsWhatItHasNoDedicatedNodeForAndTheControlScript() {
        SpiceDeck deck = parser.parse("""
                kept
                E1 out 0 in 0 10
                B1 x 0 V=V(in)*2 + 1
                K1 L1 L2 0.99
                S1 a b c d SWMOD
                .include "models.lib"
                .options reltol=1e-4
                .control
                run
                wrdata out.txt v(out)
                .endc
                .end
                R9 after the end 1
                """);
        assertInstanceOf(SpiceElement.Controlled.class, deck.statements().get(0));
        SpiceElement.Behavioural behavioural = (SpiceElement.Behavioural) deck.statements().get(1);
        assertEquals("V(in)*2 + 1", behavioural.expression());
        assertEquals(List.of("L1", "L2"), ((SpiceElement.Coupling) deck.statements().get(2)).inductors());
        assertInstanceOf(SpiceElement.Generic.class, deck.statements().get(3));
        assertEquals("models.lib", ((SpiceCard.Include) deck.statements().get(4)).path());
        assertEquals("options", ((SpiceCard.Directive) deck.statements().get(5)).directive());
        assertEquals(List.of("run", "wrdata out.txt v(out)"), ((SpiceCard.Control) deck.statements().get(6)).lines());
        assertEquals(1, deck.ignoredAfterEnd());
    }

    @Test
    void refusesWhatHasNoShapeWithItsLineAndColumn() {
        SpiceParseException unclosed = assertThrows(SpiceParseException.class,
                () -> parser.parse("t\nV1 a 0 SIN(0 1 50\n"));
        assertEquals(new SpicePosition(2, 8), unclosed.getPosition());

        SpiceParseException noValue = assertThrows(SpiceParseException.class, () -> parser.parse("t\nR1 a b\n"));
        assertEquals(2, noValue.getPosition().line());

        SpiceParseException european = assertThrows(SpiceParseException.class, () -> parser.parse("t\nR1 a b 1k5\n"));
        assertEquals(new SpicePosition(2, 8), european.getPosition());

        assertThrows(SpiceParseException.class, () -> parser.parse("t\n.subckt x a b\nR1 a b 1\n"));
        assertThrows(SpiceParseException.class, () -> parser.parse("t\n.ends\n"));
        assertThrows(SpiceParseException.class, () -> parser.parse("t\n+ R1 a b 1\n"));
        assertThrows(SpiceParseException.class, () -> parser.parse("t\nC1 a b {C\n"));
    }

    @Test
    void readsEngineeringNumbersAsSpiceDoes() {
        assertEquals(1e6, SpiceNumbers.read("1meg").orElseThrow().value(), 1e-6);
        assertEquals(1e-3, SpiceNumbers.read("1M").orElseThrow().value(), 1e-18);
        assertEquals(10e-6, SpiceNumbers.read("10uF").orElseThrow().value(), 1e-18);
        assertEquals(4.7e3, SpiceNumbers.read("4.7kOhm").orElseThrow().value(), 1e-9);
        assertEquals(1e-15, SpiceNumbers.read("1F").orElseThrow().value(), 1e-30);
        assertEquals(25.4e-6, SpiceNumbers.read("1mil").orElseThrow().value(), 1e-18);
        assertEquals(-2.5, SpiceNumbers.read("-2.5V").orElseThrow().value());
        assertFalse(SpiceNumbers.read("1k5").isPresent());
        assertFalse(SpiceNumbers.read("out").isPresent());
    }

    @Test
    void printsWhatItReadsSoThatReadingItAgainGivesTheSameDeck() throws IOException {
        for (String name : List.of("rc-sine.cir", "bridge-models.cir", "opamp-subcircuit.cir")) {
            String text = resource(name);
            String printed = parser.parse(name, text).toSource();
            String reprinted = parser.parse(name, printed).toSource();
            assertEquals(printed, reprinted, name + " prints differently after a round trip");
            for (String comment : comments(text)) {
                assertTrue(printed.contains(comment), name + " lost the comment '" + comment + "'");
            }
        }
    }

    private static List<String> comments(String text) {
        return text.lines().map(String::strip).filter(line -> line.startsWith("*") && !line.equals("*"))
                .toList();
    }

    private static String resource(String name) throws IOException {
        try (InputStream stream = SpiceParserTest.class.getResourceAsStream("/decks/" + name)) {
            if (stream == null) {
                throw new IOException("no test deck " + name);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
