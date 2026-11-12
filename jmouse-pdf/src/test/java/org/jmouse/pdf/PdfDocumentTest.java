package org.jmouse.pdf;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.InflaterInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PdfDocumentTest {

    private static String latin(byte[] file) {
        return new String(file, StandardCharsets.ISO_8859_1);
    }

    @Test
    void everyObjectIsWhereTheTableSays() {
        PdfDocument document = new PdfDocument("Плата", "test");
        document.pageInMillimetres(210, 297).lineWidth(1).moveTo(0, 0).lineTo(10, 10).stroke();
        document.page(100, 100).moveTo(0, 0).lineTo(5, 0).lineTo(5, 5).close().fill();
        String file = latin(document.write());
        assertTrue(file.startsWith("%PDF-1.7\n"));
        assertTrue(file.endsWith("%%EOF\n"));
        Matcher start = Pattern.compile("startxref\n(\\d+)\n").matcher(file);
        assertTrue(start.find());
        int table = Integer.parseInt(start.group(1));
        assertTrue(file.startsWith("xref\n", table), "startxref names the table");
        Matcher entries = Pattern.compile("(\\d{10}) 00000 n \n").matcher(file.substring(table));
        int number = 0;
        while (entries.find()) {
            number++;
            assertTrue(file.startsWith(number + " 0 obj\n", Integer.parseInt(entries.group(1))), "object " + number);
        }
        assertEquals(7, number, "catalogue, tree, information and two objects a page");
        assertTrue(file.contains("/Count 2"));
    }

    @Test
    void aPageSaysWhatWasDrawn() throws IOException {
        PdfDocument document = new PdfDocument("t", "test");
        document.page(100, 100).colour(new PdfColour(1, 0, 0)).lineWidth(0.25).moveTo(1.5, 2).lineTo(-0.0, 3.10).stroke();
        byte[] file = document.write();
        String text = latin(file);
        int begin = text.indexOf("stream\n") + "stream\n".length();
        int end = text.indexOf("\nendstream");
        try (InflaterInputStream inflating = new InflaterInputStream(new ByteArrayInputStream(file, begin, end - begin))) {
            String content = new String(inflating.readAllBytes(), StandardCharsets.US_ASCII);
            assertEquals("1 J 1 j\n1 0 0 RG 1 0 0 rg\n0.25 w\n1.5 2 m\n0 3.1 l\nS\n", content);
        }
    }

    @Test
    void wordsOfAnyAlphabetSurviveInTheTitle() {
        PdfDocument document = new PdfDocument("Я", "p");
        document.page(10, 10);
        String file = latin(document.write());
        assertTrue(file.contains("/Title <FEFF042F>"));
    }

    @Test
    void wordsAreSetInAnEmbeddedFontAndCanBeCopiedOut() throws IOException {
        java.nio.file.Path found = java.nio.file.Path.of(System.getenv().getOrDefault("WINDIR", "C:/Windows"), "Fonts", "arial.ttf");
        org.junit.jupiter.api.Assumptions.assumeTrue(java.nio.file.Files.exists(found), "this machine has no such font");
        PdfFont font = PdfFont.read(java.nio.file.Files.readAllBytes(found));
        assertEquals("Arial", font.family());
        assertTrue(font.regular());
        assertTrue(font.has('Я'), "it knows Cyrillic");
        assertTrue(font.widthOf("WW", 10) > font.widthOf("ii", 10));
        assertEquals(10, font.capHeightAt(font.sizeForCapHeight(10)), 1e-9);
        PdfDocument document = new PdfDocument("t", "p").embed(font);
        document.page(200, 200).text(font, 12, 10, 10, 0, "Плата");
        String file = latin(document.write());
        assertTrue(file.contains("/Subtype /Type0"));
        assertTrue(file.contains("/Encoding /Identity-H"));
        assertTrue(file.contains("/FontFile2"));
        assertTrue(file.contains("/ToUnicode"));
        assertTrue(file.contains("/Font << /F0"), "every page says the fonts it may use");
        assertEquals(1, Pattern.compile("/Count 1").matcher(file).results().count());
    }

    @Test
    void whatIsNoFontOrNotEmbeddedIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> PdfFont.read(new byte[64]));
        assertThrows(IllegalArgumentException.class, () -> PdfFont.read("not a font".getBytes(StandardCharsets.US_ASCII)));
    }

    @Test
    void whatCannotBeAPageIsRefused() {
        assertThrows(IllegalStateException.class, () -> new PdfDocument("t", "p").write());
        assertThrows(IllegalArgumentException.class, () -> new PdfDocument("t", "p").page(0, 10));
        assertThrows(IllegalArgumentException.class, () -> new PdfDocument("t", "p").page(1, 1).moveTo(Double.NaN, 0));
        assertThrows(IllegalArgumentException.class, () -> PdfColour.ofHex("red"));
        assertEquals(new PdfColour(1, 0, 0), PdfColour.ofHex("#ff0000"));
    }
}
