package org.jmouse.pdf;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * A TrueType font read from its bytes: what a PDF needs to embed it and to set text in it — which glyph each character is, how
 * wide each glyph stands, the font's own measures. It reads OpenType with TrueType outlines; CFF outlines, collections and a
 * font whose licence forbids embedding are refused, as a PDF could not carry them as it should.
 */
public final class PdfFont {

    private static final int TAG_LENGTH = 4;
    private static final int RESTRICTED_EMBEDDING = 0x0002;
    private static final int FAMILY_NAME = 1;
    private static final int STYLE_NAME = 2;
    private static final int WINDOWS_PLATFORM = 3;
    private static final int UNICODE_PLATFORM = 0;
    private static final int BOLD_WEIGHT = 700;

    /** A cap height for a font too old to say its own, as a share of its em. */
    private static final double USUAL_CAP_SHARE = 0.7;

    private final byte[]                bytes;
    private final String                family;
    private final String                style;
    private final int                   unitsPerEm;
    private final int                   ascent;
    private final int                   descent;
    private final int                   capHeight;
    private final int                   weight;
    private final int[]                 box;
    private final int[]                 advances;
    private final Map<Integer, Integer> glyphs;

    private PdfFont(byte[] bytes, String family, String style, int unitsPerEm, int ascent, int descent, int capHeight, int weight, int[] box, int[] advances,
                    Map<Integer, Integer> glyphs) {
        this.bytes = bytes;
        this.family = family;
        this.style = style;
        this.unitsPerEm = unitsPerEm;
        this.ascent = ascent;
        this.descent = descent;
        this.capHeight = capHeight;
        this.weight = weight;
        this.box = box;
        this.advances = advances;
        this.glyphs = glyphs;
    }

    /**
     * @param bytes the font file
     * @throws IllegalArgumentException when it is not a font this can embed, with why
     */
    public static PdfFont read(byte[] bytes) {
        ByteBuffer file = ByteBuffer.wrap(bytes);
        try {
            Map<String, Integer> tables = tablesOf(file);
            for (String needed : new String[]{"glyf", "head", "hhea", "hmtx", "maxp", "cmap", "name"}) {
                if (!tables.containsKey(needed)) {
                    throw new IllegalArgumentException("This font has no '" + needed + "' table: only TrueType outlines are embedded.");
                }
            }
            int head = tables.get("head");
            int unitsPerEm = file.getShort(head + 18) & 0xFFFF;
            int[] box = {file.getShort(head + 36), file.getShort(head + 38), file.getShort(head + 40), file.getShort(head + 42)};
            int hhea = tables.get("hhea");
            int glyphCount = file.getShort(tables.get("maxp") + 4) & 0xFFFF;
            int[] advances = advancesOf(file, tables.get("hmtx"), file.getShort(hhea + 34) & 0xFFFF, glyphCount);
            int capHeight = (int) Math.round(unitsPerEm * USUAL_CAP_SHARE);
            int weight = 0;
            Integer os2 = tables.get("OS/2");
            if (os2 != null) {
                if ((file.getShort(os2 + 8) & RESTRICTED_EMBEDDING) != 0 && (file.getShort(os2 + 8) & 0x000C) == 0) {
                    throw new IllegalArgumentException("This font's licence forbids embedding it.");
                }
                weight = file.getShort(os2 + 4) & 0xFFFF;
                if ((file.getShort(os2) & 0xFFFF) >= 2 && file.limit() > os2 + 90 && file.getShort(os2 + 88) > 0) {
                    capHeight = file.getShort(os2 + 88);
                }
            }
            int names = tables.get("name");
            return new PdfFont(bytes, nameOf(file, names, FAMILY_NAME), nameOf(file, names, STYLE_NAME), unitsPerEm, file.getShort(hhea + 4), file.getShort(hhea + 6), capHeight,
                    weight, box, advances, glyphsOf(file, tables.get("cmap")));
        } catch (IndexOutOfBoundsException | java.nio.BufferUnderflowException truncated) {
            throw new IllegalArgumentException("This font file is cut short.", truncated);
        }
    }

    public String family() {
        return family;
    }

    public String style() {
        return style;
    }

    /** Whether it is the bold of its family. */
    public boolean bold() {
        return weight >= BOLD_WEIGHT || style.toLowerCase(java.util.Locale.ROOT).contains("bold");
    }

    /** Whether it is upright and of regular weight: the face a family's text is set in. */
    public boolean regular() {
        String lower = style.toLowerCase(java.util.Locale.ROOT);
        return !bold() && (lower.equals("regular") || lower.equals("normal") || lower.equals("roman") || lower.equals("book"));
    }

    /** How wide words stand, at a size in any unit, in the same unit. */
    public double widthOf(String text, double size) {
        long units = text.codePoints().map(point -> advance(glyphOf(point))).asLongStream().sum();
        return units * size / unitsPerEm;
    }

    /** How high a capital stands at a size, in the same unit. */
    public double capHeightAt(double size) {
        return capHeight * size / unitsPerEm;
    }

    /** The size at which a capital stands so high. */
    public double sizeForCapHeight(double height) {
        return height * unitsPerEm / capHeight;
    }

    /** The glyph of a character; 0, the font's missing-glyph box, for one it does not have. */
    int glyphOf(int character) {
        return glyphs.getOrDefault(character, 0);
    }

    /** Whether it has a character's glyph. */
    public boolean has(int character) {
        return glyphs.containsKey(character);
    }

    int advance(int glyph) {
        return advances[Math.min(glyph, advances.length - 1)];
    }

    /** A width in a PDF's thousandths of its em. */
    int advanceInThousandths(int glyph) {
        return Math.round(advance(glyph) * 1000f / unitsPerEm);
    }

    byte[] bytes() {
        return bytes;
    }

    int inThousandths(int units) {
        return Math.round(units * 1000f / unitsPerEm);
    }

    int ascent() {
        return ascent;
    }

    int descent() {
        return descent;
    }

    int capHeight() {
        return capHeight;
    }

    int[] box() {
        return box.clone();
    }

    /** The name a PDF knows it by: no spaces, nothing a name may not hold. */
    String postScriptName() {
        return (family + "-" + style).replaceAll("[^A-Za-z0-9-]", "");
    }

    private static Map<String, Integer> tablesOf(ByteBuffer file) {
        int version = file.getInt(0);
        if (version != 0x00010000 && version != 0x74727565) {
            throw new IllegalArgumentException("This is no TrueType font: only TrueType outlines are embedded, and collections are not read.");
        }
        int count = file.getShort(4) & 0xFFFF;
        Map<String, Integer> tables = new HashMap<>();
        for (int index = 0; index < count; index++) {
            int record = 12 + index * 16;
            byte[] tag = new byte[TAG_LENGTH];
            file.get(record, tag);
            tables.put(new String(tag, StandardCharsets.US_ASCII), file.getInt(record + 8));
        }
        return tables;
    }

    private static int[] advancesOf(ByteBuffer file, int table, int metrics, int glyphCount) {
        int[] advances = new int[Math.max(glyphCount, 1)];
        int last = 0;
        for (int glyph = 0; glyph < advances.length; glyph++) {
            if (glyph < metrics) {
                last = file.getShort(table + glyph * 4) & 0xFFFF;
            }
            advances[glyph] = last;
        }
        return advances;
    }

    private static String nameOf(ByteBuffer file, int table, int wanted) {
        int count = file.getShort(table + 2) & 0xFFFF;
        int strings = table + (file.getShort(table + 4) & 0xFFFF);
        Optional<String> fallback = Optional.empty();
        for (int index = 0; index < count; index++) {
            int record = table + 6 + index * 12;
            if ((file.getShort(record + 6) & 0xFFFF) != wanted) {
                continue;
            }
            int platform = file.getShort(record) & 0xFFFF;
            byte[] raw = new byte[file.getShort(record + 8) & 0xFFFF];
            file.get(strings + (file.getShort(record + 10) & 0xFFFF), raw);
            if (platform == WINDOWS_PLATFORM || platform == UNICODE_PLATFORM) {
                return new String(raw, StandardCharsets.UTF_16BE);
            }
            fallback = Optional.of(new String(raw, StandardCharsets.ISO_8859_1));
        }
        return fallback.orElse("");
    }

    /** Which glyph each character is: the widest Unicode table the font has, of the two formats fonts use. */
    private static Map<Integer, Integer> glyphsOf(ByteBuffer file, int table) {
        int count = file.getShort(table + 2) & 0xFFFF;
        int best = -1;
        int bestRank = -1;
        for (int index = 0; index < count; index++) {
            int record = table + 4 + index * 8;
            int platform = file.getShort(record) & 0xFFFF;
            int encoding = file.getShort(record + 2) & 0xFFFF;
            int offset = table + file.getInt(record + 4);
            int format = file.getShort(offset) & 0xFFFF;
            boolean unicode = platform == UNICODE_PLATFORM || (platform == WINDOWS_PLATFORM && (encoding == 1 || encoding == 10));
            int rank = !unicode ? -1 : format == 12 ? 2 : format == 4 ? 1 : -1;
            if (rank > bestRank) {
                best = offset;
                bestRank = rank;
            }
        }
        if (best < 0) {
            throw new IllegalArgumentException("This font has no Unicode table of its characters.");
        }
        return (file.getShort(best) & 0xFFFF) == 12 ? format12(file, best) : format4(file, best);
    }

    private static Map<Integer, Integer> format12(ByteBuffer file, int table) {
        Map<Integer, Integer> glyphs = new HashMap<>();
        int groups = file.getInt(table + 12);
        for (int group = 0; group < groups; group++) {
            int record = table + 16 + group * 12;
            int first = file.getInt(record);
            int last = file.getInt(record + 4);
            int glyph = file.getInt(record + 8);
            for (int character = first; character <= last; character++) {
                glyphs.put(character, glyph + character - first);
            }
        }
        return glyphs;
    }

    private static Map<Integer, Integer> format4(ByteBuffer file, int table) {
        Map<Integer, Integer> glyphs = new HashMap<>();
        int segments = (file.getShort(table + 6) & 0xFFFF) / 2;
        int ends = table + 14;
        int starts = ends + segments * 2 + 2;
        int deltas = starts + segments * 2;
        int offsets = deltas + segments * 2;
        for (int segment = 0; segment < segments; segment++) {
            int end = file.getShort(ends + segment * 2) & 0xFFFF;
            int start = file.getShort(starts + segment * 2) & 0xFFFF;
            int delta = file.getShort(deltas + segment * 2);
            int offset = file.getShort(offsets + segment * 2) & 0xFFFF;
            for (int character = start; character <= end && character != 0xFFFF; character++) {
                int glyph;
                if (offset == 0) {
                    glyph = (character + delta) & 0xFFFF;
                } else {
                    glyph = file.getShort(offsets + segment * 2 + offset + (character - start) * 2) & 0xFFFF;
                    glyph = glyph == 0 ? 0 : (glyph + delta) & 0xFFFF;
                }
                if (glyph != 0) {
                    glyphs.put(character, glyph);
                }
            }
        }
        return glyphs;
    }
}
