package org.jmouse.kicad.parser;

import org.jmouse.kicad.parser.node.KiCadAtom;
import org.jmouse.kicad.parser.node.KiCadList;
import org.jmouse.kicad.parser.node.KiCadPosition;
import org.jmouse.kicad.parser.node.KiCadString;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KiCadParserTest {

    private final KiCadParser parser = new KiCadParser();
    private final KiCadPrinter printer = new KiCadPrinter();

    /** KiCad 8: tabs, texts in quotes, points on one line. */
    private static final String VERSION_8 = """
            (kicad_sch
            \t(version 20231120)
            \t(generator "eeschema")
            \t(uuid "6ef96d86-765e-492a-9886-4246a52f8b24")
            \t(wire
            \t\t(pts
            \t\t\t(xy 77.47 44.45) (xy 77.47 62.23)
            \t\t)
            \t\t(uuid "005036e4-2fe6-49db-b3a7-3ef09acde819")
            \t)
            \t(property "Value" "say \\"4.7k\\"" (at 1.27 -2.54 0) (effects (font (size 1.27 1.27)) hide))
            )
            """;

    /** KiCad 7: spaces, bare words where KiCad 8 quotes. */
    private static final String VERSION_7 = """
            (kicad_sch (version 20230121) (generator eeschema)

              (uuid 547908ee-e508-4590-8475-1787ebf4958a)

              (paper "A4")
            )
            """;

    @Test
    @DisplayName("a file nobody changed is given back byte for byte, in either version's layout")
    void givesAnUntouchedFileBack() {
        assertEquals(VERSION_8, printer.print(parser.parse("eight.kicad_sch", VERSION_8)));
        assertEquals(VERSION_7, printer.print(parser.parse("seven.kicad_sch", VERSION_7)));
    }

    @Test
    @DisplayName("lists are found by name, and words read as KiCad writes them")
    void readsTheTree() {
        KiCadList root = parser.parse("eight.kicad_sch", VERSION_8).root();
        assertEquals("kicad_sch", root.name());
        assertEquals(20231120, root.requireList("version").number(1));
        assertEquals("eeschema", root.wordOf("generator").orElseThrow());
        KiCadList points = root.requireList("wire").requireList("pts");
        assertEquals(2, points.lists("xy").size());
        assertEquals(62.23, points.lists("xy").get(1).number(2));
        KiCadList property = root.requireList("property");
        assertEquals("say \"4.7k\"", property.word(2), "the escapes are read");
        assertTrue(property.requireList("effects").says("hide"), "KiCad 7's bare 'hide' says yes");
        assertEquals("eeschema", parser.parse("seven.kicad_sch", VERSION_7).root().wordOf("generator").orElseThrow(),
                "a bare word and a text read alike");
    }

    @Test
    @DisplayName("a list a program built is laid out as KiCad 8 lays its files out")
    void laysOutABuiltList() {
        KiCadList wire = KiCadList.of("wire",
                KiCadList.of("pts", KiCadList.of("xy", KiCadAtom.of(0), KiCadAtom.of(0)), KiCadList.of("xy", KiCadAtom.of(2.54), KiCadAtom.of(-1.5))),
                KiCadList.of("uuid", KiCadString.of("a\"b")));
        assertEquals("""
                (wire
                \t(pts
                \t\t(xy 0 0) (xy 2.54 -1.5)
                \t)
                \t(uuid "a\\"b")
                )""", printer.print(wire));
    }

    @Test
    @DisplayName("a built node added to a read file keeps the file's own text around it")
    void addsToARead() {
        KiCadDocument read = parser.parse("seven.kicad_sch", VERSION_7);
        KiCadDocument added = new KiCadDocument(read.name(), read.root().with(KiCadList.of("paper", KiCadString.of("A3"))), read.trailing());
        assertEquals(VERSION_7.replace("(paper \"A4\")\n)", "(paper \"A4\")\n\t(paper \"A3\")\n)"), printer.print(added));
    }

    @Test
    @DisplayName("what does not read is refused with where it stands")
    void refusesWithAPosition() {
        KiCadParseException unclosed = assertThrows(KiCadParseException.class, () -> parser.parse("cut.kicad_sch", "(kicad_sch\n  (version 1)"));
        assertEquals(new KiCadPosition(1, 1), unclosed.position());
        KiCadParseException quote = assertThrows(KiCadParseException.class, () -> parser.parse("quote.kicad_sch", "(a\n (b \"never closed)\n)"));
        assertEquals(2, quote.position().line());
        assertThrows(KiCadParseException.class, () -> parser.parse("two.kicad_sch", "(a) (b)"));
        assertThrows(KiCadParseException.class, () -> parser.parse("none.kicad_sch", "kicad_sch"));
    }

    @Test
    @DisplayName("numbers are written as KiCad writes them: no exponent, no trailing zeros")
    void writesNumbers() {
        assertEquals("0.000127", KiCadAtom.of(1.27e-4).value());
        assertEquals("90", KiCadAtom.of(90.0).value());
        assertEquals("-2.54", KiCadAtom.of(-2.54).value());
        assertEquals("0", KiCadAtom.of(-0.0).value());
    }
}
