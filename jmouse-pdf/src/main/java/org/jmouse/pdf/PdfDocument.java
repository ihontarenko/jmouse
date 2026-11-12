package org.jmouse.pdf;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

/**
 * A PDF of vector pages, written whole: pages are asked for with {@link #page}, drawn on through their {@link PdfCanvas}, and
 * the file comes from {@link #write}. It writes PDF 1.7 with a classic cross-reference table and every page's content
 * compressed; a font is embedded whole, as Identity-H with what it takes to copy its words out; it knows nothing of what the
 * lines mean.
 */
public final class PdfDocument {

    /** How many points a millimetre is. */
    public static final double POINTS_PER_MILLIMETRE = 72 / 25.4;

    private static final int CATALOG = 1;
    private static final int PAGES   = 2;
    private static final int INFO    = 3;

    /** The objects ahead of the pages': catalogue, page tree, information. */
    private static final int FIRST_PAGE = 4;

    /** How many characters one block of a font's table holds, as the format allows at most. */
    private static final int CHARACTERS_A_BLOCK = 100;

    private final String          title;
    private final String          producer;
    private final List<PdfCanvas> pages = new ArrayList<>();
    private final Map<PdfFont, Embedded> fonts = new LinkedHashMap<>();

    /** A font as this document uses it: its name on the pages, and the glyphs set in it with the character each one is. */
    private record Embedded(String resource, TreeMap<Integer, Integer> used) {
    }

    /** How many objects a font takes: its dictionary, its descendant, its descriptor, its file, its table of characters. */
    private static final int OBJECTS_OF_A_FONT = 5;

    /**
     * @param title    what a reader's window calls it
     * @param producer what made it
     */
    public PdfDocument(String title, String producer) {
        this.title = title;
        this.producer = producer;
    }

    /** A new page, last of those so far, sized in millimetres. */
    public PdfCanvas pageInMillimetres(double width, double height) {
        return page(width * POINTS_PER_MILLIMETRE, height * POINTS_PER_MILLIMETRE);
    }

    /** A new page, last of those so far, sized in points. */
    public PdfCanvas page(double width, double height) {
        PdfCanvas page = new PdfCanvas(this, width, height);
        pages.add(page);
        return page;
    }

    /** Makes a font one this document can set words in; a font asked again is the same one. */
    public PdfDocument embed(PdfFont font) {
        fonts.computeIfAbsent(font, ignored -> new Embedded("F" + fonts.size(), new TreeMap<>()));
        return this;
    }

    String resourceOf(PdfFont font) {
        return embedded(font).resource();
    }

    /** The glyph a character is set with, written down so that its width and its character go into the file. */
    int glyphUsed(PdfFont font, int character) {
        int glyph = font.glyphOf(character);
        embedded(font).used().putIfAbsent(glyph, character);
        return glyph;
    }

    private Embedded embedded(PdfFont font) {
        Embedded found = fonts.get(font);
        if (found == null) {
            throw new IllegalArgumentException("Words are set in a font the document embeds: " + font.family());
        }
        return found;
    }

    /**
     * The file.
     *
     * @throws IllegalStateException when no page was asked for: a PDF of no pages is not one a reader opens
     */
    public byte[] write() {
        if (pages.isEmpty()) {
            throw new IllegalStateException("A PDF holds at least one page.");
        }
        int firstFont = FIRST_PAGE + pages.size() * 2;
        Output output = new Output();
        output.text("%PDF-1.7\n");
        output.bytes(new byte[]{'%', (byte) 0xE2, (byte) 0xE3, (byte) 0xCF, (byte) 0xD3, '\n'});
        output.object(CATALOG, "<< /Type /Catalog /Pages " + PAGES + " 0 R >>");
        output.object(PAGES, "<< /Type /Pages /Kids [" + kids() + "] /Count " + pages.size() + " >>");
        output.object(INFO, "<< /Title " + textString(title) + " /Producer " + textString(producer) + " >>");
        for (int index = 0; index < pages.size(); index++) {
            PdfCanvas page = pages.get(index);
            int pageObject = FIRST_PAGE + index * 2;
            output.object(pageObject, "<< /Type /Page /Parent " + PAGES + " 0 R /MediaBox [0 0 " + number(page.width()) + " "
                                      + number(page.height()) + "] /Resources << " + resources(firstFont) + " >> /Contents " + (pageObject + 1) + " 0 R >>");
            output.stream(pageObject + 1, compressed(page.content()), "");
        }
        int fontObject = firstFont;
        for (Map.Entry<PdfFont, Embedded> font : fonts.entrySet()) {
            writeFont(output, fontObject, font.getKey(), font.getValue());
            fontObject += OBJECTS_OF_A_FONT;
        }
        output.crossReference(INFO);
        return output.toBytes();
    }

    private String resources(int firstFont) {
        if (fonts.isEmpty()) {
            return "";
        }
        StringBuilder written = new StringBuilder("/Font << ");
        int object = firstFont;
        for (Embedded font : fonts.values()) {
            written.append('/').append(font.resource()).append(' ').append(object).append(" 0 R ");
            object += OBJECTS_OF_A_FONT;
        }
        return written.append(">>").toString();
    }

    private static void writeFont(Output output, int first, PdfFont font, Embedded use) {
        String name = font.postScriptName();
        output.object(first, "<< /Type /Font /Subtype /Type0 /BaseFont /" + name + " /Encoding /Identity-H /DescendantFonts [" + (first + 1) + " 0 R] /ToUnicode " + (first + 4) + " 0 R >>");
        StringBuilder widths = new StringBuilder();
        use.used().keySet().forEach(glyph -> widths.append(glyph).append(" [").append(font.advanceInThousandths(glyph)).append("] "));
        output.object(first + 1, "<< /Type /Font /Subtype /CIDFontType2 /BaseFont /" + name + " /CIDSystemInfo << /Registry (Adobe) /Ordering (Identity) /Supplement 0 >> /FontDescriptor "
                                 + (first + 2) + " 0 R /CIDToGIDMap /Identity /W [" + widths + "] >>");
        int[] box = font.box();
        output.object(first + 2, "<< /Type /FontDescriptor /FontName /" + name + " /Flags 4 /FontBBox [" + font.inThousandths(box[0]) + " " + font.inThousandths(box[1]) + " "
                                 + font.inThousandths(box[2]) + " " + font.inThousandths(box[3]) + "] /ItalicAngle 0 /Ascent " + font.inThousandths(font.ascent()) + " /Descent "
                                 + font.inThousandths(font.descent()) + " /CapHeight " + font.inThousandths(font.capHeight()) + " /StemV 80 /FontFile2 " + (first + 3) + " 0 R >>");
        output.stream(first + 3, compressed(font.bytes()), " /Length1 " + font.bytes().length);
        output.stream(first + 4, compressed(charactersOf(use).getBytes(StandardCharsets.US_ASCII)), "");
    }

    /** What lets a reader search and copy words set by glyph number: which character each glyph is. */
    private static String charactersOf(Embedded use) {
        StringBuilder map = new StringBuilder("/CIDInit /ProcSet findresource begin\n12 dict begin\nbegincmap\n/CMapName /Adobe-Identity-UCS def\n/CMapType 2 def\n1 begincodespacerange\n<0000> <FFFF>\nendcodespacerange\n");
        List<Map.Entry<Integer, Integer>> entries = new ArrayList<>(use.used().entrySet());
        for (int from = 0; from < entries.size(); from += CHARACTERS_A_BLOCK) {
            List<Map.Entry<Integer, Integer>> block = entries.subList(from, Math.min(from + CHARACTERS_A_BLOCK, entries.size()));
            map.append(block.size()).append(" beginbfchar\n");
            for (Map.Entry<Integer, Integer> entry : block) {
                StringBuilder utf16 = new StringBuilder();
                for (char unit : Character.toChars(entry.getValue())) {
                    utf16.append(String.format(Locale.ROOT, "%04X", (int) unit));
                }
                map.append(String.format(Locale.ROOT, "<%04X> <%s>\n", entry.getKey(), utf16));
            }
            map.append("endbfchar\n");
        }
        return map.append("endcmap\nCMapName currentdict /CMap defineresource pop\nend\nend\n").toString();
    }

    private String kids() {
        List<String> kids = new ArrayList<>();
        for (int index = 0; index < pages.size(); index++) {
            kids.add((FIRST_PAGE + index * 2) + " 0 R");
        }
        return String.join(" ", kids);
    }

    private static byte[] compressed(String content) {
        return compressed(content.getBytes(StandardCharsets.US_ASCII));
    }

    private static byte[] compressed(byte[] content) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DeflaterOutputStream deflating = new DeflaterOutputStream(bytes, new Deflater(Deflater.BEST_COMPRESSION))) {
            deflating.write(content);
        } catch (IOException failed) {
            throw new UncheckedIOException("A page's content could not be compressed", failed);
        }
        return bytes.toByteArray();
    }

    /** A text string as PDF wants it: UTF-16 with its byte order mark, written in hexadecimal, so any words survive. */
    private static String textString(String text) {
        StringBuilder written = new StringBuilder("<FEFF");
        for (byte each : (text == null ? "" : text).getBytes(StandardCharsets.UTF_16BE)) {
            written.append(String.format(Locale.ROOT, "%02X", each & 0xFF));
        }
        return written.append('>').toString();
    }

    private static String number(double value) {
        return String.format(Locale.ROOT, "%.3f", value);
    }

    /** The file as it grows, remembering where each object begins for the cross-reference table. */
    private static final class Output {

        private final ByteArrayOutputStream bytes   = new ByteArrayOutputStream();
        private final List<Integer>         offsets = new ArrayList<>();

        void text(String text) {
            bytes(text.getBytes(StandardCharsets.US_ASCII));
        }

        void bytes(byte[] content) {
            bytes.writeBytes(content);
        }

        void object(int number, String body) {
            begin(number);
            text(body + "\nendobj\n");
        }

        void stream(int number, byte[] content, String besides) {
            begin(number);
            text("<< /Length " + content.length + " /Filter /FlateDecode" + besides + " >>\nstream\n");
            bytes(content);
            text("\nendstream\nendobj\n");
        }

        void crossReference(int information) {
            int table = bytes.size();
            StringBuilder written = new StringBuilder("xref\n0 " + (offsets.size() + 1) + "\n0000000000 65535 f \n");
            for (int offset : offsets) {
                written.append(String.format(Locale.ROOT, "%010d 00000 n \n", offset));
            }
            written.append("trailer\n<< /Size ").append(offsets.size() + 1).append(" /Root ").append(CATALOG).append(" 0 R /Info ")
                    .append(information).append(" 0 R >>\nstartxref\n").append(table).append("\n%%EOF\n");
            text(written.toString());
        }

        byte[] toBytes() {
            return bytes.toByteArray();
        }

        private void begin(int number) {
            if (number != offsets.size() + 1) {
                throw new IllegalStateException("Objects are written in their order; " + number + " came after " + offsets.size());
            }
            offsets.add(bytes.size());
            text(number + " 0 obj\n");
        }
    }
}
